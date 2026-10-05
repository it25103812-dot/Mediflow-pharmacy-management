package com.mediflow.service;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.StockAdjustmentRequest;
import com.mediflow.entity.Inventory;
import com.mediflow.entity.MedicineBatch;
import com.mediflow.entity.StockAdjustment;
import com.mediflow.entity.User;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.InventoryRepository;
import com.mediflow.repository.MedicineBatchRepository;
import com.mediflow.repository.StockAdjustmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockAdjustmentRepository adjustmentRepository;
    private final AuditService auditService;
    private final UserService userService;

    @org.springframework.beans.factory.annotation.Value("${mediflow.expiry.warning-days:90}")
    private int warningDays;

    public InventoryService(InventoryRepository inventoryRepository,
                            MedicineBatchRepository batchRepository,
                            StockAdjustmentRepository adjustmentRepository,
                            AuditService auditService,
                            UserService userService) {
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.auditService = auditService;
        this.userService = userService;
    }

    public PageResponse<Inventory> search(String search, Long medicineId, Boolean lowStock,
                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "batch.medicine.name", "batch.expiryDate"));
        Page<Inventory> result = inventoryRepository.search(normalize(search), medicineId, lowStock, pageable);
        return PageResponse.from(result);
    }

    public Map<String, Object> alerts() {
        LocalDate today = LocalDate.now();
        Map<String, Object> map = new HashMap<>();
        map.put("lowStock", inventoryRepository.countLowStock());
        map.put("expired", batchRepository.countByExpiryDateBefore(today));
        map.put("nearExpiry", batchRepository.countByExpiryDateBetween(today, today.plusDays(warningDays)));
        List<Inventory> expiryAlerts = inventoryRepository.findExpiryAlerts(today, today.plusDays(warningDays));
        map.put("items", expiryAlerts.stream().map(this::alertRow).toList());
        return map;
    }

    private Map<String, Object> alertRow(Inventory i) {
        LocalDate today = LocalDate.now();
        MedicineBatch b = i.getBatch();
        Map<String, Object> row = new HashMap<>();
        row.put("inventoryId", i.getId());
        row.put("batchId", b.getId());
        row.put("medicineName", b.getMedicine().getName());
        row.put("batchNumber", b.getBatchNumber());
        row.put("expiryDate", b.getExpiryDate().toString());
        row.put("quantityAvailable", i.getQuantityAvailable());
        row.put("reorderLevel", i.getReorderLevel());
        row.put("status", b.getExpiryDate().isBefore(today) ? "EXPIRED"
                : b.getExpiryDate().isBefore(today.plusDays(warningDays)) ? "NEAR_EXPIRY"
                : i.getQuantityAvailable() <= i.getReorderLevel() ? "LOW_STOCK" : "OK");
        return row;
    }

    public Page<StockAdjustment> adjustmentHistory(Long batchId, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adjustmentRepository.search(batchId, normalize(type), pageable);
    }

    /** Damage / return / correction / expired-disposal adjustment with full history trail. */
    @Transactional
    public StockAdjustment adjust(StockAdjustmentRequest req) {
        MedicineBatch batch = batchRepository.findById(req.getBatchId())
                .orElseThrow(() -> ApiException.notFound("Batch not found: " + req.getBatchId()));

        Inventory inv = inventoryRepository.findByBatchId(req.getBatchId())
                .orElseThrow(() -> ApiException.badRequest("No inventory record exists for this batch"));

        if (req.getQuantityChange() == 0) {
            throw ApiException.badRequest("Quantity change cannot be zero");
        }
        int newQty = inv.getQuantityAvailable() + req.getQuantityChange();
        if (newQty < 0) {
            throw ApiException.badRequest("Adjustment would make stock negative (available: "
                    + inv.getQuantityAvailable() + ", change: " + req.getQuantityChange() + ")");
        }
        inv.setQuantityAvailable(newQty);
        inventoryRepository.save(inv);

        User performer = userService.currentUser();
        StockAdjustment adj = new StockAdjustment();
        adj.setBatch(batch);
        adj.setAdjustmentType(req.getAdjustmentType());
        adj.setQuantityChange(req.getQuantityChange());
        adj.setReason(req.getReason());
        adj.setPerformedBy(performer);
        StockAdjustment saved = adjustmentRepository.save(adj);

        auditService.log("STOCK_ADJUSTED", "INVENTORY", batch.getBatchNumber(),
                req.getAdjustmentType() + " " + (req.getQuantityChange() > 0 ? "+" : "")
                        + req.getQuantityChange() + " -> " + newQty);
        return saved;
    }

    // ---- Internal methods used by GRN and Sales services ----

    @Transactional
    public void increaseStock(Long batchId, int quantity) {
        Inventory inv = inventoryRepository.findByBatchId(batchId)
                .orElseThrow(() -> ApiException.badRequest("No inventory record for batch " + batchId));
        inv.setQuantityAvailable(inv.getQuantityAvailable() + quantity);
        inventoryRepository.save(inv);
    }

    @Transactional
    public void decreaseStock(Long batchId, int quantity) {
        // Pessimistic lock prevents two cashiers overselling the same batch
        Inventory inv = inventoryRepository.findByBatchIdForUpdate(batchId)
                .orElseThrow(() -> ApiException.badRequest("No inventory record for batch " + batchId));
        if (inv.getQuantityAvailable() < quantity) {
            throw ApiException.badRequest("Insufficient stock: requested " + quantity
                    + ", available " + inv.getQuantityAvailable());
        }
        inv.setQuantityAvailable(inv.getQuantityAvailable() - quantity);
        inventoryRepository.save(inv);
    }

    public Integer availableQuantity(Long batchId) {
        return inventoryRepository.findByBatchId(batchId)
                .map(Inventory::getQuantityAvailable).orElse(0);
    }

    public Long sumTotalStock() {
        Long v = inventoryRepository.sumTotalStock();
        return v == null ? 0L : v;
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
