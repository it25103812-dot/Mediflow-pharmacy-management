package com.mediflow.repository;

import com.mediflow.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    boolean existsByCustomerId(Long customerId);

    @Query("""
           SELECT p FROM Prescription p
           WHERE (:search IS NULL OR LOWER(p.prescriptionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(p.doctorName) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:customerId IS NULL OR p.customer.id = :customerId)
           """)
    Page<Prescription> search(@Param("search") String search,
                              @Param("customerId") Long customerId,
                              Pageable pageable);
}
