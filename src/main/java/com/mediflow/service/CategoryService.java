package com.mediflow.service;

import com.mediflow.dto.CategoryRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Category;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.CategoryRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public CategoryService(CategoryRepository categoryRepository, AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    public PageResponse<Category> search(String search, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.from(categoryRepository.search(normalize(search), active, pageable));
    }

    public List<Category> findAllActive() {
        return categoryRepository.search(null, true, PageRequest.of(0, 500, Sort.by("name"))).getContent();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found: " + id));
    }

    @Transactional
    public Category create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(trimOrNull(req.description()));
        category.setActive(req.active() == null || req.active());
        Category saved = categoryRepository.save(category);
        auditService.log("CATEGORY_CREATED", "CATEGORY", saved.getId().toString(), saved.getName());
        return saved;
    }

    @Transactional
    public Category update(Long id, CategoryRequest req) {
        Category existing = getById(id);
        String name = req.name().trim();
        if (!existing.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A category with this name already exists");
        }
        existing.setName(name);
        existing.setDescription(trimOrNull(req.description()));
        if (req.active() != null) {
            existing.setActive(req.active());
        }
        Category saved = categoryRepository.save(existing);
        auditService.log("CATEGORY_UPDATED", "CATEGORY", saved.getId().toString(), saved.getName());
        return saved;
    }

    /** Hard-deletes when unused; deactivates when referenced by medicines. Returns true if really deleted. */
    public boolean delete(Long id) {
        Category c = getById(id);
        String name = c.getName();
        try {
            categoryRepository.deleteById(id);
            categoryRepository.flush();
            auditService.log("CATEGORY_DELETED", "CATEGORY", id.toString(), name);
            return true;
        } catch (DataAccessException ex) {
            Category fresh = getById(id);
            fresh.setActive(false);
            categoryRepository.save(fresh);
            auditService.log("CATEGORY_DEACTIVATED", "CATEGORY", id.toString(), name);
            return false;
        }
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
