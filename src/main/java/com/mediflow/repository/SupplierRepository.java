package com.mediflow.repository;

import com.mediflow.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);

    @Query("""
           SELECT s FROM Supplier s
           WHERE (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:active IS NULL OR s.active = :active)
           """)
    Page<Supplier> search(@Param("search") String search,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
