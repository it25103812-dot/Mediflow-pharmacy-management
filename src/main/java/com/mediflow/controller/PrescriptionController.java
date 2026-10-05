package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.PrescriptionRequest;
import com.mediflow.entity.Prescription;
import com.mediflow.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public PageResponse<Prescription> list(@RequestParam(defaultValue = "") String search,
                                           @RequestParam(required = false) Long customerId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return prescriptionService.search(search, customerId, page, size);
    }

    @GetMapping("/{id}")
    public Prescription get(@PathVariable Long id) {
        return prescriptionService.getById(id);
    }

    @PostMapping
    public Prescription create(@Valid @RequestBody PrescriptionRequest request) {
        return prescriptionService.create(request);
    }

    @PutMapping("/{id}")
    public Prescription update(@PathVariable Long id, @Valid @RequestBody PrescriptionRequest request) {
        return prescriptionService.update(id, request);
    }
}
