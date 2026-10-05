package com.mediflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** Finance edit of a bill: only money-related fields (items/stock are never touched). */
public record SaleUpdateRequest(
        @NotNull @Pattern(regexp = "CASH|CARD|ONLINE") String paymentMethod,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discount) {
}
