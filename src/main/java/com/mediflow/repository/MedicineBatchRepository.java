package com.mediflow.repository;

import com.mediflow.entity.MedicineBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {

    List<MedicineBatch> findByMedicineIdOrderByIdAsc(Long medicineId);

    boolean existsByMedicineIdAndBatchNumberIgnoreCaseAndIdNot(Long medicineId, String batchNumber, Long id);

    @Query("""
           SELECT b FROM MedicineBatch b
           WHERE (:search IS NULL OR LOWER(b.batchNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(b.medicine.name) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:medicineId IS NULL OR b.medicine.id = :medicineId)
           """)
    Page<MedicineBatch> search(@Param("search") String search,
                               @Param("medicineId") Long medicineId,
                               Pageable pageable);

    // Expiry alert queries (used by dashboard + reports)
    long countByExpiryDateBefore(LocalDate date);

    long countByExpiryDateBetween(LocalDate start, LocalDate end);

    @Query("""
           SELECT b FROM MedicineBatch b
           WHERE b.expiryDate < :today
              OR b.expiryDate < :warningDate
           ORDER BY b.expiryDate ASC
           """)
    List<MedicineBatch> findExpiryAlerts(@Param("today") LocalDate today,
                                         @Param("warningDate") LocalDate warningDate);
}
