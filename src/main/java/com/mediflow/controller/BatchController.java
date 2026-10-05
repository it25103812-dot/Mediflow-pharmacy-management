package com.mediflow.controller;

import com.mediflow.dto.BatchDto;
import com.mediflow.dto.BatchRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.service.BatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public PageResponse<BatchDto> list(@RequestParam(defaultValue = "") String search,
                                       @RequestParam(required = false) Long medicineId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return batchService.search(search, medicineId, page, size);
    }

    @GetMapping("/medicine/{medicineId}")
    public List<BatchDto> byMedicine(@PathVariable Long medicineId) {
        return batchService.listByMedicine(medicineId);
    }

    @PostMapping
    public BatchDto create(@Valid @RequestBody BatchRequest request) {
        return batchService.getDtoById(batchService.create(request).getId());
    }

    @PutMapping("/{id}")
    public BatchDto update(@PathVariable Long id, @Valid @RequestBody BatchRequest request) {
        return batchService.getDtoById(batchService.update(id, request).getId());
    }

    @GetMapping("/expiry-counts")
    public Map<String, Object> expiryCounts() {
        return batchService.expiryCounts();
    }
}
