package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class PrescriptionRequest {
    private Long customerId;
    @NotBlank @Size(max = 120) private String doctorName;
    private LocalDate prescriptionDate;
    private Long medicineId;
    @Size(max = 500) private String notes;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public LocalDate getPrescriptionDate() { return prescriptionDate; }
    public void setPrescriptionDate(LocalDate prescriptionDate) { this.prescriptionDate = prescriptionDate; }
    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
