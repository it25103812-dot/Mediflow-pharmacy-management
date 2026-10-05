package com.mediflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public class GrnRequest {
    @NotNull private Long purchaseOrderId;
    @Size(max = 500) private String notes;
    @NotEmpty @Valid private List<GrnItemRequest> items;

    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<GrnItemRequest> getItems() { return items; }
    public void setItems(List<GrnItemRequest> items) { this.items = items; }
}
