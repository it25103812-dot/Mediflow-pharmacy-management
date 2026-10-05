package com.mediflow.service;

import com.mediflow.dto.MedicineRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Category;
import com.mediflow.entity.Medicine;
import com.mediflow.entity.PriceHistory;
import com.mediflow.entity.User;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.CategoryRepository;
import com.mediflow.repository.MedicineRepository;
import com.mediflow.repository.PriceHistoryRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final CategoryRepository categoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AuditService auditService;
    private final UserService userService;

    public MedicineService(MedicineRepository medicineRepository,
                           CategoryRepository categoryRepository,
                           PriceHistoryRepository priceHistoryRepository,
                           AuditService auditService,
                           UserService userService) {
        this.medicineRepository = medicineRepository;
        this.categoryRepository = categoryRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.auditService = auditService;
        this.userService = userService;
    }

    public PageResponse<Medicine> search(String search, Long categoryId, Boolean active,
                                         Boolean prescriptionRequired, int page, int size,
                                         String sortBy, String sortDir) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                mapSortField(sortBy));
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Medicine> result = medicineRepository.search(normalize(search), categoryId, active,
                prescriptionRequired, pageable);
        return PageResponse.from(result);
    }

    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Medicine not found: " + id));
    }

    public List<PriceHistory> getPriceHistory(Long medicineId) {
        return priceHistoryRepository.findByMedicineIdOrderByChangedAtDesc(medicineId);
    }

    @Transactional
    public Medicine create(MedicineRequest req) {
        if (medicineRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw ApiException.conflict("A medicine with this name already exists");
        }
        Medicine m = new Medicine();
        applyRequest(m, req);
        m.setActive(req.active() == null || req.active());
        Medicine saved = medicineRepository.save(m);
        auditService.log("MEDICINE_CREATED", "MEDICINE", saved.getId().toString(), saved.getName());
        return saved;
    }

    @Transactional
    public Medicine update(Long id, MedicineRequest req) {
        Medicine m = getById(id);
        if (medicineRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw ApiException.conflict("A medicine with this name already exists");
        }
        applyRequest(m, req);
        if (req.active() != null) {
            m.setActive(req.active());
        }
        Medicine saved = medicineRepository.save(m);
        auditService.log("MEDICINE_UPDATED", "MEDICINE", saved.getId().toString(), saved.getName());
        return saved;
    }

    /**
     * Tries to permanently delete the medicine. If it is already referenced by batches,
     * orders, sales, price history etc. the database refuses (foreign keys), so we fall
     * back to deactivating it instead. Returns true if the row was really deleted.
     * (Intentionally not @Transactional so a failed delete does not poison the fallback.)
     */
    public boolean delete(Long id) {
        Medicine m = getById(id);
        String name = m.getName();
        try {
            medicineRepository.deleteById(id);
            medicineRepository.flush();
            auditService.log("MEDICINE_DELETED", "MEDICINE", id.toString(), name);
            return true;
        } catch (DataAccessException ex) {
            Medicine fresh = getById(id);
            fresh.setActive(false);
            medicineRepository.save(fresh);
            auditService.log("MEDICINE_DEACTIVATED", "MEDICINE", id.toString(), name);
            return false;
        }
    }

    private void applyRequest(Medicine m, MedicineRequest req) {
        m.setName(req.name().trim());
        m.setGenericName(trimOrNull(req.genericName()));
        m.setCategory(resolveCategory(req.categoryId()));
        m.setManufacturer(trimOrNull(req.manufacturer()));
        m.setDescription(trimOrNull(req.description()));
        m.setPrescriptionRequired(Boolean.TRUE.equals(req.prescriptionRequired()));

        BigDecimal newPrice = req.unitPrice();
        if (m.getId() != null && m.getUnitPrice() != null
                && m.getUnitPrice().compareTo(newPrice) != 0) {
            // Preserve the old price before overwriting - price history is never lost
            User changedBy = userService.currentUser();
            PriceHistory ph = new PriceHistory();
            ph.setMedicine(m);
            ph.setOldPrice(m.getUnitPrice());
            ph.setNewPrice(newPrice);
            ph.setChangedBy(changedBy);
            priceHistoryRepository.save(ph);
        }
        m.setUnitPrice(newPrice);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.badRequest("Category not found: " + categoryId));
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy == null ? "name" : sortBy) {
            case "unitPrice" -> "unitPrice";
            case "createdAt" -> "createdAt";
            case "id" -> "id";
            default -> "name";
        };
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
