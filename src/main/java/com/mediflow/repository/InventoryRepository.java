package com.mediflow.repository;

import com.mediflow.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByBatchId(Long batchId);

    // Pessimistic lock – used inside sale transactions to prevent overselling
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.batch.id = :batchId")
    Optional<Inventory> findByBatchIdForUpdate(@Param("batchId") Long batchId);

    @Query("""
           SELECT i FROM Inventory i
           WHERE (:search IS NULL OR LOWER(i.batch.medicine.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(i.batch.batchNumber) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:medicineId IS NULL OR i.batch.medicine.id = :medicineId)
             AND (:lowStock IS NULL
                  OR (:lowStock = true  AND i.quantityAvailable <= i.reorderLevel)
                  OR (:lowStock = false AND i.quantityAvailable >  i.reorderLevel))
           """)
    Page<Inventory> search(@Param("search") String search,
                           @Param("medicineId") Long medicineId,
                           @Param("lowStock") Boolean lowStock,
                           Pageable pageable);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantityAvailable <= i.reorderLevel")
    long countLowStock();

    @Query("""
           SELECT i FROM Inventory i
           WHERE i.quantityAvailable > 0
             AND (i.batch.expiryDate < :today OR i.batch.expiryDate < :warningDate)
           ORDER BY i.batch.expiryDate ASC
           """)
    List<Inventory> findExpiryAlerts(@Param("today") LocalDate today,
                                     @Param("warningDate") LocalDate warningDate);

    @Query("SELECT COALESCE(SUM(i.quantityAvailable), 0) FROM Inventory i")
    Long sumTotalStock();
}
