package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MedicineRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String genericName,
        Long categoryId,
        @Size(max = 120) String manufacturer,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @Size(max = 500) String description,
        Boolean prescriptionRequired,
        Boolean active) {
}
