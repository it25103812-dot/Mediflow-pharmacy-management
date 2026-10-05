package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.PurchaseOrderRequest;
import com.mediflow.entity.PurchaseOrder;
import com.mediflow.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    public PageResponse<PurchaseOrder> list(@RequestParam(defaultValue = "") String search,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return purchaseOrderService.search(search, status, page, size);
    }

    @GetMapping("/{id}")
    public PurchaseOrder get(@PathVariable Long id) {
        return purchaseOrderService.getById(id);
    }

    @PostMapping
    public PurchaseOrder create(@Valid @RequestBody PurchaseOrderRequest request) {
        return purchaseOrderService.create(request);
    }

    @PutMapping("/{id}")
    public PurchaseOrder update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest request) {
        return purchaseOrderService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public PurchaseOrder changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return purchaseOrderService.changeStatus(id, body.get("status"));
    }
}
