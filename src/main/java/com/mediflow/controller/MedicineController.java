package com.mediflow.controller;

import com.mediflow.dto.MedicineRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Medicine;
import com.mediflow.entity.PriceHistory;
import com.mediflow.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    public PageResponse<Medicine> list(@RequestParam(defaultValue = "") String search,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Boolean active,
                                       @RequestParam(required = false) Boolean prescriptionRequired,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "name") String sortBy,
                                       @RequestParam(defaultValue = "asc") String sortDir) {
        return medicineService.search(search, categoryId, active, prescriptionRequired,
                page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public Medicine get(@PathVariable Long id) {
        return medicineService.getById(id);
    }

    @GetMapping("/{id}/price-history")
    public List<PriceHistory> priceHistory(@PathVariable Long id) {
        return medicineService.getPriceHistory(id);
    }

    @PostMapping
    public Medicine create(@Valid @RequestBody MedicineRequest request) {
        return medicineService.create(request);
    }

    @PutMapping("/{id}")
    public Medicine update(@PathVariable Long id, @Valid @RequestBody MedicineRequest request) {
        return medicineService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String, Object> delete(@PathVariable Long id) {
        boolean deleted = medicineService.delete(id);
        return java.util.Map.of("deleted", deleted);
    }
}
