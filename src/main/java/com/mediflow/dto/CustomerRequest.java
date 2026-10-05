package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CustomerRequest {
    @NotBlank @Size(max = 120)
    @Pattern(regexp = Patterns.HAS_LETTER, message = Patterns.HAS_LETTER_MSG)
    private String name;
    @Size(max = 30) @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MSG)
    private String phone;
    @Size(max = 100) @Pattern(regexp = Patterns.EMAIL, message = Patterns.EMAIL_MSG)
    private String email;
    @Size(max = 255) private String address;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
