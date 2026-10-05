package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.SaleDto;
import com.mediflow.dto.SaleRequest;
import com.mediflow.dto.SaleUpdateRequest;
import com.mediflow.entity.Sale;
import com.mediflow.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public PageResponse<Sale> list(@RequestParam(defaultValue = "") String search,
                                   @RequestParam(required = false) Long customerId,
                                   @RequestParam(required = false) Long cashierId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String start,
                                   @RequestParam(required = false) String end,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return saleService.search(search, customerId, cashierId, status,
                parse(start), end != null ? LocalDateTime.parse(end + "T23:59:59") : null,
                page, size);
    }

    @GetMapping("/{id}")
    public SaleDto get(@PathVariable Long id) {
        return saleService.getDtoById(id);
    }

    @PostMapping
    public SaleDto create(@Valid @RequestBody SaleRequest request) {
        return saleService.completeSale(request);
    }

    @PutMapping("/{id}")
    public SaleDto update(@PathVariable Long id, @Valid @RequestBody SaleUpdateRequest request) {
        return saleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String, Object> delete(@PathVariable Long id) {
        saleService.delete(id);
        return java.util.Map.of("deleted", true);
    }

    private LocalDateTime parse(String date) {
        return (date == null || date.isBlank()) ? null : LocalDateTime.parse(date + "T00:00:00");
    }
}
