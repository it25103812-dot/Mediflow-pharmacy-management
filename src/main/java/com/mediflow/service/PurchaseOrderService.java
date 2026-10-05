package com.mediflow.service;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.PurchaseOrderRequest;
import com.mediflow.entity.Medicine;
import com.mediflow.entity.PurchaseOrder;
import com.mediflow.entity.PurchaseOrderItem;
import com.mediflow.entity.Supplier;
import com.mediflow.entity.User;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.MedicineRepository;
import com.mediflow.repository.PurchaseOrderRepository;
import com.mediflow.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final AuditService auditService;
    private final UserService userService;

    public PurchaseOrderService(PurchaseOrderRepository poRepository,
                                SupplierRepository supplierRepository,
                                MedicineRepository medicineRepository,
                                AuditService auditService,
                                UserService userService) {
        this.poRepository = poRepository;
        this.supplierRepository = supplierRepository;
        this.medicineRepository = medicineRepository;
        this.auditService = auditService;
        this.userService = userService;
    }

    public PageResponse<PurchaseOrder> search(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));
        return PageResponse.from(poRepository.search(normalize(search), normalize(status), pageable));
    }

    public PurchaseOrder getById(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Purchase order not found: " + id));
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrderRequest req) {
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> ApiException.badRequest("Supplier not found: " + req.getSupplierId()));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrder.STATUS_DRAFT);
        po.setExpectedDate(req.getExpectedDate());
        po.setNotes(req.getNotes());
        po.setCreatedBy(userService.currentUser());
        po.setPoNumber(nextPoNumber());
        po.setOrderDate(java.time.LocalDateTime.now());
        applyItems(po, req.getItems());
        po.setTotalAmount(po.getGrandTotal());

        PurchaseOrder saved = poRepository.save(po);
        auditService.log("PO_CREATED", "PURCHASE_ORDER", saved.getPoNumber(),
                "Supplier: " + supplier.getName() + ", items: " + saved.getItems().size());
        return saved;
    }

    @Transactional
    public PurchaseOrder update(Long id, PurchaseOrderRequest req) {
        PurchaseOrder po = getById(id);
        if (!PurchaseOrder.STATUS_DRAFT.equals(po.getStatus())) {
            throw ApiException.badRequest("Only DRAFT purchase orders can be edited");
        }
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> ApiException.badRequest("Supplier not found: " + req.getSupplierId()));

        po.setSupplier(supplier);
        po.setExpectedDate(req.getExpectedDate());
        po.setNotes(req.getNotes());
        applyItems(po, req.getItems());
        po.setTotalAmount(po.getGrandTotal());

        PurchaseOrder saved = poRepository.save(po);
        auditService.log("PO_UPDATED", "PURCHASE_ORDER", saved.getPoNumber(), "Draft updated");
        return saved;
    }

    @Transactional
    public PurchaseOrder changeStatus(Long id, String newStatus) {
        PurchaseOrder po = getById(id);
        String current = po.getStatus();

        boolean valid = switch (newStatus) {
            case PurchaseOrder.STATUS_SENT -> PurchaseOrder.STATUS_DRAFT.equals(current);
            case PurchaseOrder.STATUS_CANCELLED ->
                    PurchaseOrder.STATUS_DRAFT.equals(current) || PurchaseOrder.STATUS_SENT.equals(current);
            default -> false;
        };
        if (!valid) {
            throw ApiException.badRequest("Cannot change status from " + current + " to " + newStatus);
        }
        po.setStatus(newStatus);
        PurchaseOrder saved = poRepository.save(po);
        auditService.log("PO_STATUS_CHANGED", "PURCHASE_ORDER", saved.getPoNumber(),
                current + " -> " + newStatus);
        return saved;
    }

    /** Called by GrnService when a GRN is verified; recalculates receiving status. */
    @Transactional
    public void recalcReceivingStatus(Long poId) {
        PurchaseOrder po = getById(poId);
        if (PurchaseOrder.STATUS_CANCELLED.equals(po.getStatus())) {
            throw ApiException.badRequest("Cannot receive stock against a cancelled purchase order");
        }
        boolean anyReceived = false;
        boolean fullyReceived = true;
        for (PurchaseOrderItem item : po.getItems()) {
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            int recv = item.getReceivedQuantity() == null ? 0 : item.getReceivedQuantity();
            if (recv > 0) anyReceived = true;
            if (recv < qty) fullyReceived = false;
        }
        String newStatus = fullyReceived ? PurchaseOrder.STATUS_RECEIVED
                : anyReceived ? PurchaseOrder.STATUS_PARTIALLY_RECEIVED
                : po.getStatus();
        if (!newStatus.equals(po.getStatus())) {
            String oldStatus = po.getStatus();
            po.setStatus(newStatus);
            poRepository.save(po);
            auditService.log("PO_STATUS_CHANGED", "PURCHASE_ORDER", po.getPoNumber(),
                    oldStatus + " -> " + newStatus + " (GRN verified)");
        }
    }

    public long countPending() {
        return poRepository.countByStatusIn(List.of(PurchaseOrder.STATUS_DRAFT, PurchaseOrder.STATUS_SENT,
                PurchaseOrder.STATUS_PARTIALLY_RECEIVED));
    }

    private void applyItems(PurchaseOrder po, List<com.mediflow.dto.PoItemRequest> items) {
        po.getItems().clear();
        for (com.mediflow.dto.PoItemRequest reqItem : items) {
            Medicine medicine = medicineRepository.findById(reqItem.getMedicineId())
                    .orElseThrow(() -> ApiException.badRequest("Medicine not found: " + reqItem.getMedicineId()));
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setMedicine(medicine);
            item.setQuantity(reqItem.getQuantity());
            item.setReceivedQuantity(0);
            item.setPurchasePrice(reqItem.getPurchasePrice());
            item.setLineTotal(reqItem.getPurchasePrice()
                    .multiply(BigDecimal.valueOf(reqItem.getQuantity())));
            po.addItem(item);
        }
    }

    private String nextPoNumber() {
        String prefix = "PO-" + java.time.LocalDate.now().getYear() + "-";
        return prefix + String.format("%04d", poRepository.count() + 1);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
