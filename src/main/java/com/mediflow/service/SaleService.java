package com.mediflow.service;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.SaleRequest;
import com.mediflow.dto.SaleUpdateRequest;
import com.mediflow.dto.SaleDto;
import com.mediflow.exception.ApiException;
import com.mediflow.entity.Customer;
import com.mediflow.entity.Inventory;
import com.mediflow.entity.MedicineBatch;
import com.mediflow.entity.Payment;
import com.mediflow.entity.Prescription;
import com.mediflow.entity.Sale;
import com.mediflow.entity.SaleItem;
import com.mediflow.entity.User;
import com.mediflow.repository.CustomerRepository;
import com.mediflow.repository.InventoryRepository;
import com.mediflow.repository.MedicineBatchRepository;
import com.mediflow.repository.PaymentRepository;
import com.mediflow.repository.PrescriptionRepository;
import com.mediflow.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final MedicineBatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final BatchService batchService;
    private final AuditService auditService;
    private final UserService userService;

    @org.springframework.beans.factory.annotation.Value("${mediflow.expiry.warning-days:90}")
    private int warningDays;

    public SaleService(SaleRepository saleRepository,
                       MedicineBatchRepository batchRepository,
                       InventoryRepository inventoryRepository,
                       CustomerRepository customerRepository,
                       PrescriptionRepository prescriptionRepository,
                       PaymentRepository paymentRepository,
                       InventoryService inventoryService,
                       BatchService batchService,
                       AuditService auditService,
                       UserService userService) {
        this.saleRepository = saleRepository;
        this.batchRepository = batchRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerRepository = customerRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
        this.batchService = batchService;
        this.auditService = auditService;
        this.userService = userService;
    }

    public PageResponse<Sale> search(String search, Long customerId, Long cashierId, String status,
                                     LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleDate"));
        return PageResponse.from(saleRepository.search(normalize(search), customerId, cashierId,
                normalize(status), start, end, pageable));
    }

    public Sale getById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sale not found: " + id));
    }

    public SaleDto getDtoById(Long id) {
        return SaleDto.from(getById(id));
    }

    /**
     * Transactional POS checkout. Validates every business rule, deducts stock with a
     * pessimistic lock (no overselling / negative stock) and records the payment.
     */
    @Transactional
    public SaleDto completeSale(SaleRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        Sale sale = new Sale();
        sale.setInvoiceNumber(nextInvoiceNumber());
        sale.setStatus(Sale.STATUS_COMPLETED);
        sale.setPaymentMethod(req.getPaymentMethod());
        sale.setCashier(userService.currentUser());
        sale.setSaleDate(java.time.LocalDateTime.now());

        if (req.getCustomerId() != null) {
            Customer customer = customerRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> ApiException.badRequest("Customer not found: " + req.getCustomerId()));
            sale.setCustomer(customer);
        }

        Prescription prescription = null;
        if (req.getPrescriptionId() != null) {
            prescription = prescriptionRepository.findById(req.getPrescriptionId())
                    .orElseThrow(() -> ApiException.badRequest("Prescription not found: " + req.getPrescriptionId()));
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (SaleRequest.SaleItemRequest itemReq : req.getItems()) {
            MedicineBatch batch = batchRepository.findById(itemReq.getBatchId())
                    .orElseThrow(() -> ApiException.badRequest("Batch not found: " + itemReq.getBatchId()));

            // Rule 1: expired medicine cannot be sold
            if (batch.getExpiryDate().isBefore(LocalDate.now())) {
                throw ApiException.badRequest("Medicine '" + batch.getMedicine().getName()
                        + "' batch " + batch.getBatchNumber() + " is expired and cannot be sold");
            }

            // Rule 2: unit price comes from the batch, not the client
            BigDecimal unitPrice = batch.getSellingPrice();
            if (itemReq.getUnitPrice() != null
                    && itemReq.getUnitPrice().compareTo(unitPrice) != 0) {
                throw ApiException.badRequest("Unit price mismatch for " + batch.getMedicine().getName()
                        + "; expected " + unitPrice);
            }

            // Rule 3: prescription-required medicine must reference a prescription
            if (Boolean.TRUE.equals(batch.getMedicine().getPrescriptionRequired())
                    && prescription == null) {
                throw ApiException.badRequest("Medicine '" + batch.getMedicine().getName()
                        + "' requires a prescription. Add prescription information before checkout.");
            }

            // Rule 4: stock check with pessimistic lock (prevents overselling)
            Inventory inv = inventoryRepository.findByBatchIdForUpdate(itemReq.getBatchId())
                    .orElseThrow(() -> ApiException.badRequest("No inventory record for batch "
                            + batch.getBatchNumber()));
            if (inv.getQuantityAvailable() < itemReq.getQuantity()) {
                throw ApiException.badRequest("Insufficient stock for '" + batch.getMedicine().getName()
                        + "' batch " + batch.getBatchNumber() + ": requested " + itemReq.getQuantity()
                        + ", available " + inv.getQuantityAvailable());
            }

            SaleItem si = new SaleItem();
            si.setMedicine(batch.getMedicine());
            si.setBatch(batch);
            si.setQuantity(itemReq.getQuantity());
            si.setUnitPrice(unitPrice);
            si.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            sale.addItem(si);

            subtotal = subtotal.add(si.getLineTotal());
        }

        BigDecimal discount = req.getDiscount() == null ? BigDecimal.ZERO : req.getDiscount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Discount cannot be negative");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw ApiException.badRequest("Discount cannot exceed the subtotal");
        }

        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(subtotal.subtract(discount));
        sale.setPrescription(prescription);
        Sale saved = saleRepository.save(sale);

        // Deduct stock AFTER the sale row exists - same transaction, rolled back on any failure
        for (SaleItem si : saved.getItems()) {
            inventoryService.decreaseStock(si.getBatch().getId(), si.getQuantity());
        }

        Payment payment = new Payment();
        payment.setSale(saved);
        payment.setAmount(saved.getTotal());
        payment.setMethod(req.getPaymentMethod());
        payment.setPaidAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);

        auditService.log("SALE_COMPLETED", "SALE", saved.getInvoiceNumber(),
                "Total LKR " + saved.getTotal() + " (" + saved.getItems().size() + " items)");
        return SaleDto.from(saved);
    }

    /** Finance edit: payment method + discount. Total and the linked payment record are recalculated. */
    @Transactional
    public SaleDto update(Long id, SaleUpdateRequest req) {
        Sale sale = getById(id);
        if (!Sale.STATUS_COMPLETED.equals(sale.getStatus())) {
            throw ApiException.badRequest("Only COMPLETED bills can be edited; this bill is " + sale.getStatus());
        }
        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        if (discount.compareTo(sale.getSubtotal()) > 0) {
            throw ApiException.badRequest("Discount cannot exceed the subtotal");
        }
        sale.setDiscount(discount);
        sale.setTotal(sale.getSubtotal().subtract(discount));
        sale.setPaymentMethod(req.paymentMethod());
        Sale saved = saleRepository.save(sale);

        for (Payment p : paymentRepository.findBySaleId(id)) {
            p.setAmount(saved.getTotal());
            p.setMethod(req.paymentMethod());
            paymentRepository.save(p);
        }
        auditService.log("SALE_UPDATED", "SALE", saved.getInvoiceNumber(),
                "Total LKR " + saved.getTotal() + ", " + saved.getPaymentMethod());
        return SaleDto.from(saved);
    }

    /** Deletes a bill. A COMPLETED sale puts its quantities back into stock first. */
    @Transactional
    public void delete(Long id) {
        Sale sale = getById(id);
        if (Sale.STATUS_COMPLETED.equals(sale.getStatus())) {
            for (SaleItem si : sale.getItems()) {
                inventoryService.increaseStock(si.getBatch().getId(), si.getQuantity());
            }
        }
        paymentRepository.deleteAll(paymentRepository.findBySaleId(id));
        saleRepository.delete(sale);
        auditService.log("SALE_DELETED", "SALE", sale.getInvoiceNumber(),
                "Total LKR " + sale.getTotal() + " (" + sale.getStatus() + ")");
    }

    private String nextInvoiceNumber() {
        String prefix = "INV-" + java.time.LocalDate.now().getYear() + "-";
        long n = saleRepository.count() + 1;
        // Deleting bills makes count()+1 reuse an existing number, so skip any that are taken
        while (saleRepository.existsByInvoiceNumber(prefix + String.format("%04d", n))) {
            n++;
        }
        return prefix + String.format("%04d", n);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
