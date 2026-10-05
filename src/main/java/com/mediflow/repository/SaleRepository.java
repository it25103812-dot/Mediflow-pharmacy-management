package com.mediflow.repository;

import com.mediflow.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByCustomerId(Long customerId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    long countBySaleDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
           SELECT COALESCE(SUM(s.total), 0) FROM Sale s
           WHERE s.status <> 'RETURNED'
             AND s.saleDate >= :start AND s.saleDate < :end
           """)
    BigDecimal sumTotalBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
           SELECT COALESCE(SUM(s.total), 0) FROM Sale s
           WHERE s.status <> 'RETURNED'
           """)
    BigDecimal sumAllTimeRevenue();

    @Query("""
           SELECT s FROM Sale s
           WHERE (:search IS NULL OR LOWER(s.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:customerId IS NULL OR s.customer.id = :customerId)
             AND (:cashierId IS NULL OR s.cashier.id = :cashierId)
             AND (:status IS NULL OR s.status = :status)
             AND (:start IS NULL OR s.saleDate >= :start)
             AND (:end IS NULL OR s.saleDate < :end)
           """)
    Page<Sale> search(@Param("search") String search,
                      @Param("customerId") Long customerId,
                      @Param("cashierId") Long cashierId,
                      @Param("status") String status,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end,
                      Pageable pageable);

    // Top selling medicines (by units sold) - used by dashboard + reports
    @Query(value = """
            SELECT m.id          AS medicineId,
                   m.name        AS medicineName,
                   SUM(si.quantity) AS totalQty,
                   SUM(si.line_total) AS totalRevenue
            FROM sale_items si
            JOIN medicines m ON m.id = si.medicine_id
            JOIN sales s     ON s.id = si.sale_id
            WHERE s.status <> 'RETURNED'
              AND (:start IS NULL OR s.sale_date >= :start)
              AND (:end   IS NULL OR s.sale_date < :end)
            GROUP BY m.id, m.name
            ORDER BY totalQty DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopMedicineProjection> findTopSelling(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("limit") int limit);

    interface TopMedicineProjection {
        Long getMedicineId();
        String getMedicineName();
        Long getTotalQty();
        BigDecimal getTotalRevenue();
    }
}
