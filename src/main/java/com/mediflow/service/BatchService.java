package com.mediflow.service;

import com.mediflow.dto.BatchDto;
import com.mediflow.dto.PageResponse;
import com.mediflow.dto.BatchRequest;
import com.mediflow.entity.Inventory;
import com.mediflow.entity.Medicine;
import com.mediflow.entity.MedicineBatch;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.InventoryRepository;
import com.mediflow.repository.MedicineBatchRepository;
import com.mediflow.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private final MedicineBatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditService auditService;

    @Value("${mediflow.expiry.warning-days:90}")
    private int warningDays;

    public BatchService(MedicineBatchRepository batchRepository,
                        MedicineRepository medicineRepository,
                        InventoryRepository inventoryRepository,
                        AuditService auditService) {
        this.batchRepository = batchRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditService = auditService;
    }

    public PageResponse<BatchDto> search(String search, Long medicineId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expiryDate"));
        LocalDate today = LocalDate.now();
        Page<BatchDto> result = batchRepository.search(normalize(search), medicineId, pageable)
                .map(b -> toDto(b, today));
        return PageResponse.from(result);
    }

    public List<BatchDto> listByMedicine(Long medicineId) {
        LocalDate today = LocalDate.now();
        return batchRepository.findByMedicineIdOrderByIdAsc(medicineId).stream()
                .map(b -> toDto(b, today))
                .toList();
    }

    public BatchDto getDtoById(Long id) {
        return toDto(findBatch(id), LocalDate.now());
    }

    public Map<String, Object> expiryCounts() {
        LocalDate today = LocalDate.now();
        Map<String, Object> map = new HashMap<>();
        map.put("expired", batchRepository.countByExpiryDateBefore(today));
        map.put("nearExpiry", batchRepository.countByExpiryDateBetween(today, today.plusDays(warningDays)));
        return map;
    }

    @Transactional
    public MedicineBatch create(BatchRequest req) {
        Medicine medicine = medicineRepository.findById(req.medicineId())
                .orElseThrow(() -> ApiException.badRequest("Medicine not found: " + req.medicineId()));

        if (req.expiryDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Cannot create a batch that is already expired");
        }

        batchRepository.findByMedicineIdOrderByIdAsc(req.medicineId()).stream()
                .filter(b -> b.getBatchNumber().equalsIgnoreCase(req.batchNumber().trim()))
                .findAny()
                .ifPresent(b -> { throw ApiException.conflict("Batch number already exists for this medicine"); });

        MedicineBatch batch = new MedicineBatch();
        batch.setMedicine(medicine);
        batch.setBatchNumber(req.batchNumber().trim());
        batch.setExpiryDate(req.expiryDate());
        batch.setPurchasePrice(req.purchasePrice());
        batch.setSellingPrice(req.sellingPrice());
        MedicineBatch saved = batchRepository.save(batch);

        Inventory inv = new Inventory();
        inv.setBatch(saved);
        inv.setQuantityAvailable(req.initialQuantity());
        inv.setReorderLevel(req.reorderLevel());
        inventoryRepository.save(inv);

        auditService.log("BATCH_CREATED", "BATCH", saved.getId().toString(),
                saved.getBatchNumber() + " for " + medicine.getName() + " (qty " + req.initialQuantity() + ")");
        return saved;
    }

    @Transactional
    public MedicineBatch update(Long id, BatchRequest req) {
        MedicineBatch batch = findBatch(id);
        if (batchRepository.existsByMedicineIdAndBatchNumberIgnoreCaseAndIdNot(
                batch.getMedicine().getId(), req.batchNumber().trim(), id)) {
            throw ApiException.conflict("Batch number already exists for this medicine");
        }
        if (inventoryRepository.findByBatchId(id)
                .map(i -> i.getQuantityAvailable() != null && i.getQuantityAvailable() > 0)
                .orElse(false)
                && !batch.getBatchNumber().equalsIgnoreCase(req.batchNumber().trim())) {
            throw ApiException.badRequest("Cannot renumber a batch that already holds stock");
        }
        batch.setBatchNumber(req.batchNumber().trim());
        batch.setExpiryDate(req.expiryDate());
        batch.setPurchasePrice(req.purchasePrice());
        batch.setSellingPrice(req.sellingPrice());

        Inventory inv = inventoryRepository.findByBatchId(id).orElse(null);
        if (inv != null && req.reorderLevel() != null) {
            inv.setReorderLevel(req.reorderLevel());
            inventoryRepository.save(inv);
        }
        MedicineBatch saved = batchRepository.save(batch);
        auditService.log("BATCH_UPDATED", "BATCH", id.toString(), saved.getBatchNumber());
        return saved;
    }

    private MedicineBatch findBatch(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Batch not found: " + id));
    }

    public BatchDto toDto(MedicineBatch b, LocalDate today) {
        Inventory inv = inventoryRepository.findByBatchId(b.getId()).orElse(null);
        int qty = inv == null || inv.getQuantityAvailable() == null ? 0 : inv.getQuantityAvailable();
        int reorder = inv == null || inv.getReorderLevel() == null ? 20 : inv.getReorderLevel();
        String status;
        if (b.isExpired(today)) status = "EXPIRED";
        else if (b.isNearExpiry(today, warningDays)) status = "NEAR_EXPIRY";
        else if (qty <= reorder) status = "LOW_STOCK";
        else status = "OK";
        return new BatchDto(b.getId(), b.getMedicine().getId(), b.getMedicine().getName(),
                b.getBatchNumber(), b.getExpiryDate(), qty, reorder,
                b.getPurchasePrice(), b.getSellingPrice(), status);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
