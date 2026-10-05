package com.mediflow.repository;

import com.mediflow.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
           SELECT m FROM Medicine m
           WHERE (:search IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:categoryId IS NULL OR m.category.id = :categoryId)
             AND (:active IS NULL OR m.active = :active)
             AND (:prescriptionRequired IS NULL OR m.prescriptionRequired = :prescriptionRequired)
           """)
    Page<Medicine> search(@Param("search") String search,
                          @Param("categoryId") Long categoryId,
                          @Param("active") Boolean active,
                          @Param("prescriptionRequired") Boolean prescriptionRequired,
                          Pageable pageable);

    List<Medicine> findTop50ByActiveTrueOrderByNameAsc();
}
