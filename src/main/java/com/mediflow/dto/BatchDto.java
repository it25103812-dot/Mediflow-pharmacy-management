package com.mediflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchDto(
        Long id,
        Long medicineId,
        String medicineName,
        String batchNumber,
        LocalDate expiryDate,
        Integer quantityAvailable,
        Integer reorderLevel,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        String stockStatus) {
}
