package com.mediflow.repository;

import com.mediflow.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
           SELECT c FROM Customer c
           WHERE (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))
           """)
    Page<Customer> search(@Param("search") String search, Pageable pageable);
}
