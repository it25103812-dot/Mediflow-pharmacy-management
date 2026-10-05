package com.mediflow.repository;

import com.mediflow.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByMedicineIdOrderByChangedAtDesc(Long medicineId);
}
