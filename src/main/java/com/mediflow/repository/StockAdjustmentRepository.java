package com.mediflow.repository;

import com.mediflow.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    @Query("""
           SELECT sa FROM StockAdjustment sa
           WHERE (:batchId IS NULL OR sa.batch.id = :batchId)
             AND (:type IS NULL OR sa.adjustmentType = :type)
           """)
    Page<StockAdjustment> search(@Param("batchId") Long batchId,
                                 @Param("type") String type,
                                 Pageable pageable);
}
