package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchRequest(
        @NotNull Long medicineId,
        @NotBlank @Size(max = 50) String batchNumber,
        @NotNull LocalDate expiryDate,
        @NotNull @DecimalMin("0.00") BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.00") BigDecimal sellingPrice,
        @NotNull @Min(0) Integer initialQuantity,
        @NotNull @Min(0) Integer reorderLevel) {
}
