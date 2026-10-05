package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.SupplierRequest;
import com.mediflow.entity.Supplier;
import com.mediflow.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public PageResponse<Supplier> list(@RequestParam(defaultValue = "") String search,
                                       @RequestParam(required = false) Boolean active,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return supplierService.search(search, active, page, size);
    }

    @GetMapping("/{id}")
    public Supplier get(@PathVariable Long id) {
        return supplierService.getById(id);
    }

    @PostMapping
    public Supplier create(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String, Object> delete(@PathVariable Long id) {
        boolean deleted = supplierService.delete(id);
        return java.util.Map.of("deleted", deleted);
    }
}
