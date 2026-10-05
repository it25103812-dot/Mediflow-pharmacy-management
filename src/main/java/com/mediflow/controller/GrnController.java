package com.mediflow.controller;

import com.mediflow.dto.GrnRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.GoodsReceivedNote;
import com.mediflow.service.GrnService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/grn")
public class GrnController {

    private final GrnService grnService;

    public GrnController(GrnService grnService) {
        this.grnService = grnService;
    }

    @GetMapping
    public PageResponse<GoodsReceivedNote> list(@RequestParam(defaultValue = "") String search,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(grnService.search(search, status, page, size));
    }

    @GetMapping("/{id}")
    public GoodsReceivedNote get(@PathVariable Long id) {
        return grnService.getById(id);
    }

    @PostMapping
    public GoodsReceivedNote create(@Valid @RequestBody GrnRequest request) {
        return grnService.create(request);
    }

    @PatchMapping("/{id}/verify")
    public GoodsReceivedNote verify(@PathVariable Long id) {
        return grnService.verify(id);
    }

    @PatchMapping("/{id}/cancel")
    public GoodsReceivedNote cancel(@PathVariable Long id) {
        return grnService.cancel(id);
    }
}
