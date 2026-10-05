package com.mediflow.dto;

import com.mediflow.entity.MedicineBatch;
import com.mediflow.entity.Sale;
import com.mediflow.entity.SaleItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleDto(
        Long id,
        String invoiceNumber,
        Long customerId,
        String customerName,
        Long cashierId,
        String cashierName,
        LocalDateTime saleDate,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        String status,
        String prescriptionNumber,
        List<SaleItemDto> items) {

    public record SaleItemDto(
            Long id,
            Long medicineId,
            String medicineName,
            Long batchId,
            String batchNumber,
            String expiryDate,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {

        public static SaleItemDto from(SaleItem si) {
            MedicineBatch b = si.getBatch();
            return new SaleItemDto(si.getId(),
                    si.getMedicine().getId(),
                    si.getMedicine().getName(),
                    b.getId(),
                    b.getBatchNumber(),
                    b.getExpiryDate() == null ? null : b.getExpiryDate().toString(),
                    si.getQuantity(), si.getUnitPrice(), si.getLineTotal());
        }
    }

    public static SaleDto from(Sale s) {
        return new SaleDto(s.getId(), s.getInvoiceNumber(),
                s.getCustomer() != null ? s.getCustomer().getId() : null,
                s.getCustomer() != null ? s.getCustomer().getName() : "Walk-in Customer",
                s.getCashier() != null ? s.getCashier().getId() : null,
                s.getCashier() != null ? s.getCashier().getFullName() : null,
                s.getSaleDate(), s.getSubtotal(), s.getDiscount(), s.getTotal(),
                s.getPaymentMethod(), s.getStatus(),
                s.getPrescription() != null ? s.getPrescription().getPrescriptionNumber() : null,
                s.getItems().stream().map(SaleItemDto::from).toList());
    }
}
