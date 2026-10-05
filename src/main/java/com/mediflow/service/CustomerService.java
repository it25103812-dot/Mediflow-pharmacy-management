package com.mediflow.service;

import com.mediflow.dto.CustomerRequest;
import com.mediflow.dto.PageResponse;
import com.mediflow.entity.Customer;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.CustomerRepository;
import com.mediflow.repository.PrescriptionRepository;
import com.mediflow.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository,
                           SaleRepository saleRepository,
                           PrescriptionRepository prescriptionRepository,
                           AuditService auditService) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.auditService = auditService;
    }

    public PageResponse<Customer> search(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.from(customerRepository.search(normalize(search), pageable));
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Customer not found: " + id));
    }

    @Transactional
    public Customer create(CustomerRequest req) {
        Customer c = new Customer();
        apply(c, req);
        Customer saved = customerRepository.save(c);
        auditService.log("CUSTOMER_CREATED", "CUSTOMER", saved.getId().toString(), saved.getName());
        return saved;
    }

    @Transactional
    public Customer update(Long id, CustomerRequest req) {
        Customer c = getById(id);
        apply(c, req);
        Customer saved = customerRepository.save(c);
        auditService.log("CUSTOMER_UPDATED", "CUSTOMER", saved.getId().toString(), saved.getName());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Customer c = getById(id);
        if (saleRepository.existsByCustomerId(id) || prescriptionRepository.existsByCustomerId(id)) {
            throw ApiException.conflict(
                    "This customer has sales or prescriptions on record and cannot be deleted");
        }
        customerRepository.delete(c);
        auditService.log("CUSTOMER_DELETED", "CUSTOMER", id.toString(), c.getName());
    }

    private void apply(Customer c, CustomerRequest req) {
        c.setName(req.getName().trim());
        c.setPhone(com.mediflow.dto.Patterns.normalizePhone(req.getPhone()));
        c.setEmail(trimOrNull(req.getEmail()));
        c.setAddress(trimOrNull(req.getAddress()));
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
