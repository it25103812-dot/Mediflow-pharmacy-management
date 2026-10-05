package com.mediflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderRequest {
    @NotNull private Long supplierId;
    private LocalDate expectedDate;
    @Size(max = 500) private String notes;
    @NotEmpty @Valid private List<PoItemRequest> items;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<PoItemRequest> getItems() { return items; }
    public void setItems(List<PoItemRequest> items) { this.items = items; }
}
