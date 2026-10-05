package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.StockAdjustmentRequest;
import com.mediflow.entity.Inventory;
import com.mediflow.entity.StockAdjustment;
import com.mediflow.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public PageResponse<Inventory> list(@RequestParam(defaultValue = "") String search,
                                        @RequestParam(required = false) Long medicineId,
                                        @RequestParam(required = false) Boolean lowStock,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return inventoryService.search(search, medicineId, lowStock, page, size);
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        return inventoryService.alerts();
    }

    @GetMapping("/adjustments")
    public PageResponse<StockAdjustment> adjustments(@RequestParam(required = false) Long batchId,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(inventoryService.adjustmentHistory(batchId, type, page, size));
    }

    @PostMapping("/adjustments")
    public StockAdjustment adjust(@Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.adjust(request);
    }
}
