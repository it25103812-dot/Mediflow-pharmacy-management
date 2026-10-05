package com.mediflow.repository;

import com.mediflow.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    long countByStatus(String status);

    long countByStatusIn(java.util.Collection<String> statuses);

    @Query("""
           SELECT po FROM PurchaseOrder po
           WHERE (:search IS NULL OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(po.supplier.name) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:status IS NULL OR po.status = :status)
           """)
    Page<PurchaseOrder> search(@Param("search") String search,
                               @Param("status") String status,
                               Pageable pageable);
}
