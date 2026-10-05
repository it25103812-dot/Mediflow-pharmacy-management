package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UserRequest {
    @NotBlank @Size(max = 50) @Pattern(regexp = Patterns.PERSON_NAME, message = "First name: " + Patterns.PERSON_NAME_MSG)
    private String firstName;
    @NotBlank @Size(max = 50) @Pattern(regexp = Patterns.PERSON_NAME, message = "Last name: " + Patterns.PERSON_NAME_MSG)
    private String lastName;
    @NotBlank @Size(max = 100) @Pattern(regexp = Patterns.EMAIL, message = Patterns.EMAIL_MSG)
    private String email;
    @Pattern(regexp = "^$|.{8,100}", message = "Password must be 8 to 100 characters")
    private String password; // required on create, blank = keep current on update
    @NotNull private String roleName;
    private Boolean active = true;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
