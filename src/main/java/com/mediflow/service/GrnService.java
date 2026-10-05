package com.mediflow.service;

import com.mediflow.dto.GrnRequest;
import com.mediflow.entity.GoodsReceivedNote;
import com.mediflow.entity.GoodsReceivedNoteItem;
import com.mediflow.entity.Inventory;
import com.mediflow.entity.Medicine;
import com.mediflow.entity.MedicineBatch;
import com.mediflow.entity.PurchaseOrder;
import com.mediflow.entity.Supplier;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.GoodsReceivedNoteRepository;
import com.mediflow.repository.InventoryRepository;
import com.mediflow.repository.MedicineBatchRepository;
import com.mediflow.repository.MedicineRepository;
import com.mediflow.repository.PurchaseOrderRepository;
import com.mediflow.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GrnService {

    private final GoodsReceivedNoteRepository grnRepository;
    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final PurchaseOrderService purchaseOrderService;
    private final AuditService auditService;
    private final UserService userService;

    public GrnService(GoodsReceivedNoteRepository grnRepository,
                      PurchaseOrderRepository poRepository,
                      SupplierRepository supplierRepository,
                      MedicineRepository medicineRepository,
                      MedicineBatchRepository batchRepository,
                      InventoryRepository inventoryRepository,
                      InventoryService inventoryService,
                      PurchaseOrderService purchaseOrderService,
                      AuditService auditService,
                      UserService userService) {
        this.grnRepository = grnRepository;
        this.poRepository = poRepository;
        this.supplierRepository = supplierRepository;
        this.medicineRepository = medicineRepository;
        this.batchRepository = batchRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.purchaseOrderService = purchaseOrderService;
        this.auditService = auditService;
        this.userService = userService;
    }

    public Page<GoodsReceivedNote> search(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedDate"));
        return grnRepository.search(normalize(search), normalize(status), pageable);
    }

    public GoodsReceivedNote getById(Long id) {
        return grnRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("GRN not found: " + id));
    }

    @Transactional
    public GoodsReceivedNote create(GrnRequest req) {
        PurchaseOrder po = poRepository.findById(req.getPurchaseOrderId())
                .orElseThrow(() -> ApiException.badRequest("Purchase order not found: " + req.getPurchaseOrderId()));
        if (PurchaseOrder.STATUS_CANCELLED.equals(po.getStatus())) {
            throw ApiException.badRequest("Cannot create a GRN against a cancelled purchase order");
        }
        if (PurchaseOrder.STATUS_RECEIVED.equals(po.getStatus())) {
            throw ApiException.badRequest("This purchase order has already been fully received");
        }
        if (PurchaseOrder.STATUS_DRAFT.equals(po.getStatus())) {
            throw ApiException.badRequest("Send the purchase order to the supplier before receiving goods");
        }
        Supplier supplier = po.getSupplier();

        GoodsReceivedNote grn = new GoodsReceivedNote();
        grn.setGrnNumber(nextGrnNumber());
        grn.setPurchaseOrder(po);
        grn.setSupplier(supplier);
        grn.setStatus(GoodsReceivedNote.STATUS_PENDING);
        grn.setNotes(req.getNotes());
        grn.setCreatedBy(userService.currentUser());
        grn.setReceivedDate(java.time.LocalDateTime.now());

        for (com.mediflow.dto.GrnItemRequest holder : req.getItems()) {
            Medicine medicine = medicineRepository.findById(holder.getMedicineId())
                    .orElseThrow(() -> ApiException.badRequest("Medicine not found: " + holder.getMedicineId()));
            GoodsReceivedNoteItem item = new GoodsReceivedNoteItem();
            item.setMedicine(medicine);
            item.setBatchNumber(holder.getBatchNumber().trim());
            item.setExpiryDate(holder.getExpiryDate());
            item.setQuantity(holder.getQuantity());
            item.setPurchasePrice(holder.getPurchasePrice());
            item.setSellingPrice(holder.getSellingPrice());
            if (holder.getPoItemId() != null) {
                item.setPoItem(po.getItems().stream()
                        .filter(i -> i.getId().equals(holder.getPoItemId()))
                        .findFirst()
                        .orElse(null));
            }
            if (item.getPoItem() != null) {
                int received = item.getPoItem().getReceivedQuantity() == null ? 0 : item.getPoItem().getReceivedQuantity();
                int remaining = item.getPoItem().getQuantity() - received;
                if (holder.getQuantity() > remaining) {
                    throw ApiException.badRequest("Quantity for '" + medicine.getName()
                            + "' exceeds the remaining ordered quantity (" + remaining + ")");
                }
            }
            grn.addItem(item);
        }

        GoodsReceivedNote saved = grnRepository.save(grn);
        auditService.log("GRN_CREATED", "GRN", saved.getGrnNumber(),
                "Against " + po.getPoNumber() + " (" + saved.getItems().size() + " items, PENDING)");
        return saved;
    }

    /** Verification is the ONLY path that increases stock. */
    @Transactional
    public GoodsReceivedNote verify(Long id) {
        GoodsReceivedNote grn = getById(id);
        if (!GoodsReceivedNote.STATUS_PENDING.equals(grn.getStatus())) {
            throw ApiException.badRequest("Only PENDING GRNs can be verified");
        }
        if (grn.getItems().isEmpty()) {
            throw ApiException.badRequest("GRN has no items");
        }

        for (GoodsReceivedNoteItem item : grn.getItems()) {
            // 1. find or create the batch
            MedicineBatch batch = batchRepository
                    .findByMedicineIdOrderByIdAsc(item.getMedicine().getId()).stream()
                    .filter(b -> b.getBatchNumber().equalsIgnoreCase(item.getBatchNumber()))
                    .findFirst()
                    .orElseGet(() -> {
                        MedicineBatch nb = new MedicineBatch();
                        nb.setMedicine(item.getMedicine());
                        nb.setBatchNumber(item.getBatchNumber());
                        nb.setExpiryDate(item.getExpiryDate());
                        nb.setPurchasePrice(item.getPurchasePrice());
                        nb.setSellingPrice(item.getSellingPrice());
                        return batchRepository.save(nb);
                    });

            // 2. find or create inventory row, then increase stock
            Inventory inv = inventoryRepository.findByBatchId(batch.getId())
                    .orElseGet(() -> {
                        Inventory ni = new Inventory();
                        ni.setBatch(batch);
                        ni.setQuantityAvailable(0);
                        ni.setReorderLevel(20);
                        return ni;
                    });
            inv.setQuantityAvailable(inv.getQuantityAvailable() + item.getQuantity());
            inventoryRepository.save(inv);

            // 3. update received quantity on the PO line
            if (item.getPoItem() != null) {
                var poItem = item.getPoItem();
                poItem.setReceivedQuantity(
                        (poItem.getReceivedQuantity() == null ? 0 : poItem.getReceivedQuantity())
                                + item.getQuantity());
            }
        }

        grn.setStatus(GoodsReceivedNote.STATUS_VERIFIED);
        grn.setVerifiedBy(userService.currentUser());
        grn.setVerifiedAt(java.time.LocalDateTime.now());
        GoodsReceivedNote saved = grnRepository.save(grn);

        purchaseOrderService.recalcReceivingStatus(grn.getPurchaseOrder().getId());
        auditService.log("GRN_VERIFIED", "GRN", saved.getGrnNumber(),
                "Stock increased for " + saved.getItems().size() + " item(s)");
        return saved;
    }

    @Transactional
    public GoodsReceivedNote cancel(Long id) {
        GoodsReceivedNote grn = getById(id);
        if (!GoodsReceivedNote.STATUS_PENDING.equals(grn.getStatus())) {
            throw ApiException.badRequest("Only PENDING GRNs can be cancelled");
        }
        grn.setStatus(GoodsReceivedNote.STATUS_CANCELLED);
        GoodsReceivedNote saved = grnRepository.save(grn);
        auditService.log("GRN_CANCELLED", "GRN", saved.getGrnNumber(), "Pending GRN cancelled");
        return saved;
    }

    public long countPending() {
        return grnRepository.countByStatus(GoodsReceivedNote.STATUS_PENDING);
    }

    private String nextGrnNumber() {
        String prefix = "GRN-" + java.time.LocalDate.now().getYear() + "-";
        return prefix + String.format("%04d", grnRepository.count() + 1);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
