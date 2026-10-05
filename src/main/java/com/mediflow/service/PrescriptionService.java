package com.mediflow.service;

import com.mediflow.dto.PrescriptionRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Customer;
import com.mediflow.entity.Medicine;
import com.mediflow.entity.Prescription;
import com.mediflow.entity.User;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.CustomerRepository;
import com.mediflow.repository.MedicineRepository;
import com.mediflow.repository.PrescriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CustomerRepository customerRepository;
    private final MedicineRepository medicineRepository;
    private final AuditService auditService;
    private final UserService userService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               CustomerRepository customerRepository,
                               MedicineRepository medicineRepository,
                               AuditService auditService,
                               UserService userService) {
        this.prescriptionRepository = prescriptionRepository;
        this.customerRepository = customerRepository;
        this.medicineRepository = medicineRepository;
        this.auditService = auditService;
        this.userService = userService;
    }

    public PageResponse<Prescription> search(String search, Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(prescriptionRepository.search(normalize(search), customerId, pageable));
    }

    public Prescription getById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Prescription not found: " + id));
    }

    @Transactional
    public Prescription create(PrescriptionRequest req) {
        Prescription p = new Prescription();
        p.setPrescriptionNumber(nextNumber());
        apply(p, req);
        p.setCreatedBy(userService.currentUser());
        Prescription saved = prescriptionRepository.save(p);
        auditService.log("PRESCRIPTION_CREATED", "PRESCRIPTION", saved.getPrescriptionNumber(),
                saved.getDoctorName());
        return saved;
    }

    @Transactional
    public Prescription update(Long id, PrescriptionRequest req) {
        Prescription p = getById(id);
        apply(p, req);
        Prescription saved = prescriptionRepository.save(p);
        auditService.log("PRESCRIPTION_UPDATED", "PRESCRIPTION", saved.getPrescriptionNumber(), "Updated");
        return saved;
    }

    private void apply(Prescription p, PrescriptionRequest req) {
        if (req.getCustomerId() != null) {
            Customer c = customerRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> ApiException.badRequest("Customer not found: " + req.getCustomerId()));
            p.setCustomer(c);
        } else {
            p.setCustomer(null);
        }
        if (req.getMedicineId() != null) {
            Medicine m = medicineRepository.findById(req.getMedicineId())
                    .orElseThrow(() -> ApiException.badRequest("Medicine not found: " + req.getMedicineId()));
            p.setMedicine(m);
        } else {
            p.setMedicine(null);
        }
        p.setDoctorName(req.getDoctorName().trim());
        p.setPrescriptionDate(req.getPrescriptionDate() != null
                ? req.getPrescriptionDate() : java.time.LocalDate.now());
        p.setNotes(req.getNotes());
    }

    private String nextNumber() {
        return "PRX-" + java.time.LocalDate.now().getYear() + "-"
                + String.format("%04d", prescriptionRepository.count() + 1);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
