package com.mediflow.service;

import com.mediflow.dto.SupplierRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Supplier;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.SupplierRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    public SupplierService(SupplierRepository supplierRepository, AuditService auditService) {
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    public PageResponse<Supplier> search(String search, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.from(supplierRepository.search(normalize(search), active, pageable));
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier not found: " + id));
    }

    @Transactional
    public Supplier create(SupplierRequest req) {
        if (supplierRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw ApiException.conflict("A supplier with this name already exists");
        }
        Supplier s = new Supplier();
        apply(s, req);
        s.setActive(req.active() == null || req.active());
        Supplier saved = supplierRepository.save(s);
        auditService.log("SUPPLIER_CREATED", "SUPPLIER", saved.getId().toString(), saved.getName());
        return saved;
    }

    @Transactional
    public Supplier update(Long id, SupplierRequest req) {
        Supplier s = getById(id);
        if (!s.getName().equalsIgnoreCase(req.name().trim())
                && supplierRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw ApiException.conflict("A supplier with this name already exists");
        }
        apply(s, req);
        if (req.active() != null) {
            s.setActive(req.active());
        }
        Supplier saved = supplierRepository.save(s);
        auditService.log("SUPPLIER_UPDATED", "SUPPLIER", saved.getId().toString(), saved.getName());
        return saved;
    }

    /** Hard-deletes when unused; deactivates when referenced by purchase orders / GRNs. Returns true if really deleted. */
    public boolean delete(Long id) {
        Supplier s = getById(id);
        String name = s.getName();
        try {
            supplierRepository.deleteById(id);
            supplierRepository.flush();
            auditService.log("SUPPLIER_DELETED", "SUPPLIER", id.toString(), name);
            return true;
        } catch (DataAccessException ex) {
            Supplier fresh = getById(id);
            fresh.setActive(false);
            supplierRepository.save(fresh);
            auditService.log("SUPPLIER_DEACTIVATED", "SUPPLIER", id.toString(), name);
            return false;
        }
    }

    private void apply(Supplier s, SupplierRequest req) {
        s.setName(req.name().trim());
        s.setContactPerson(trimOrNull(req.contactPerson()));
        s.setPhone(com.mediflow.dto.Patterns.normalizePhone(req.phone()));
        s.setEmail(trimOrNull(req.email()));
        s.setAddress(trimOrNull(req.address()));
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
