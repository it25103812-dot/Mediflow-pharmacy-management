package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.CustomerRequest;
import com.mediflow.dto.SaleDto;
import com.mediflow.entity.Customer;
import com.mediflow.repository.SaleRepository;
import com.mediflow.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final SaleRepository saleRepository;

    public CustomerController(CustomerService customerService, SaleRepository saleRepository) {
        this.customerService = customerService;
        this.saleRepository = saleRepository;
    }

    @GetMapping
    public PageResponse<Customer> list(@RequestParam(defaultValue = "") String search,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return customerService.search(search, page, size);
    }

    @GetMapping("/{id}")
    public Customer get(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping("/{id}/purchases")
    public List<SaleDto> purchaseHistory(@PathVariable Long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleDate"));
        return saleRepository.search(null, id, null, null, null, null, pageable)
                .getContent().stream().map(SaleDto::from).toList();
    }

    @PostMapping
    public Customer create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Map.of("message", "Customer deleted");
    }
}
