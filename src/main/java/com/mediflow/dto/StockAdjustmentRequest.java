package com.mediflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StockAdjustmentRequest {
    @NotNull private Long batchId;
    @NotNull @Pattern(regexp = "DAMAGE|RETURN|CORRECTION|EXPIRED_DISPOSAL") private String adjustmentType;
    @NotNull private Integer quantityChange;
    @Size(max = 255) private String reason;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
