package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SupplierRequest(
        @NotBlank @Size(max = 150)
        @Pattern(regexp = Patterns.HAS_LETTER, message = Patterns.HAS_LETTER_MSG) String name,
        @Size(max = 100)
        @Pattern(regexp = Patterns.HAS_LETTER, message = "Contact person must contain at least one letter") String contactPerson,
        @Size(max = 30) @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MSG) String phone,
        @Size(max = 100) @Pattern(regexp = Patterns.EMAIL, message = Patterns.EMAIL_MSG) String email,
        @Size(max = 255) String address,
        Boolean active) {
}
