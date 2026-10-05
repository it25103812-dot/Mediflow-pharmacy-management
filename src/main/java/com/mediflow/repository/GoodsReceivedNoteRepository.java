package com.mediflow.repository;

import com.mediflow.entity.GoodsReceivedNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsReceivedNoteRepository extends JpaRepository<GoodsReceivedNote, Long> {

    long countByStatus(String status);

    @Query("""
           SELECT g FROM GoodsReceivedNote g
           WHERE (:search IS NULL OR LOWER(g.grnNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(g.supplier.name) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:status IS NULL OR g.status = :status)
           """)
    Page<GoodsReceivedNote> search(@Param("search") String search,
                                   @Param("status") String status,
                                   Pageable pageable);
}
