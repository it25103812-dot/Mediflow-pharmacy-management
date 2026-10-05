package com.mediflow.service;

import com.mediflow.entity.GoodsReceivedNote;
import com.mediflow.entity.Inventory;
import com.mediflow.entity.MedicineBatch;
import com.mediflow.entity.PurchaseOrder;
import com.mediflow.entity.Sale;
import com.mediflow.entity.Supplier;
import com.mediflow.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every report is built as a generic table: {title, columns, rows, totals}.
 * The same data feeds the JSON API, CSV export and PDF export.
 */
@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final InventoryRepository inventoryRepository;
    private final MedicineBatchRepository batchRepository;
    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final GoodsReceivedNoteRepository grnRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${mediflow.expiry.warning-days:90}")
    private int warningDays;

    public ReportService(SaleRepository saleRepository,
                         InventoryRepository inventoryRepository,
                         MedicineBatchRepository batchRepository,
                         PurchaseOrderRepository poRepository,
                         SupplierRepository supplierRepository,
                         GoodsReceivedNoteRepository grnRepository,
                         CustomerRepository customerRepository,
                         UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.poRepository = poRepository;
        this.supplierRepository = supplierRepository;
        this.grnRepository = grnRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> build(String type, LocalDate start, LocalDate end) {
        LocalDate s = start != null ? start : LocalDate.now().minusMonths(1);
        LocalDate e = end != null ? end : LocalDate.now();
        LocalDateTime startTs = s.atStartOfDay();
        LocalDateTime endTs = e.plusDays(1).atStartOfDay();

        return switch (type == null ? "" : type) {
            case "sales" -> salesReport(startTs, endTs);
            case "revenue" -> revenueReport(startTs, endTs);
            case "medicine-sales" -> medicineSalesReport(startTs, endTs);
            case "top-selling" -> topSellingReport(startTs, endTs);
            case "low-stock" -> lowStockReport();
            case "expiry" -> expiryReport();
            case "purchase" -> purchaseReport(startTs, endTs);
            case "supplier" -> supplierReport();
            default -> throw new com.mediflow.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Unknown report type: " + type);
        };
    }

    // ---------------------------------------------------------------- reports

    private Map<String, Object> salesReport(LocalDateTime start, LocalDateTime end) {
        List<String> columns = List.of("Invoice", "Date", "Customer", "Cashier",
                "Items", "Subtotal", "Discount", "Total", "Payment", "Status");
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long orders = 0;

        List<Sale> sales = saleRepository.findAll(Sort.by(Sort.Direction.DESC, "saleDate")).stream()
                .filter(s -> !s.getSaleDate().isBefore(start) && s.getSaleDate().isBefore(end))
                .toList();
        for (Sale s : sales) {
            rows.add(List.of(
                    s.getInvoiceNumber(),
                    s.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    s.getCustomer() != null ? s.getCustomer().getName() : "Walk-in",
                    s.getCashier() != null ? s.getCashier().getFullName() : "-",
                    s.getItems().size(),
                    s.getSubtotal(), s.getDiscount(), s.getTotal(),
                    s.getPaymentMethod(), s.getStatus()));
            if (!"RETURNED".equals(s.getStatus())) {
                totalRevenue = totalRevenue.add(s.getTotal());
                orders++;
            }
        }
        return table("Daily Sales Report", columns, rows,
                Map.of("Orders", orders, "Revenue (LKR)", totalRevenue));
    }

    private Map<String, Object> revenueReport(LocalDateTime start, LocalDateTime end) {
        List<String> columns = List.of("Date", "Orders", "Revenue (LKR)");
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal grand = BigDecimal.ZERO;
        long totalOrders = 0;
        LocalDate cursor = start.toLocalDate();
        LocalDate stop = end.toLocalDate();
        while (cursor.isBefore(stop)) {
            LocalDateTime ds = cursor.atStartOfDay();
            LocalDateTime de = ds.plusDays(1);
            BigDecimal rev = saleRepository.sumTotalBetween(ds, de);
            long orders = saleRepository.countBySaleDateBetween(ds, de);
            if (orders > 0) {
                rows.add(List.of(cursor.toString(), orders, rev));
                grand = grand.add(rev);
                totalOrders += orders;
            }
            cursor = cursor.plusDays(1);
        }
        return table("Revenue Report", columns, rows,
                Map.of("Total Orders", totalOrders, "Total Revenue (LKR)", grand));
    }

    private Map<String, Object> medicineSalesReport(LocalDateTime start, LocalDateTime end) {
        List<String> columns = List.of("Medicine", "Units Sold", "Revenue (LKR)");
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;
        for (var row : saleRepository.findTopSelling(start, end, 1000)) {
            // rows already filtered by the query's date bounds
            rows.add(List.of(row.getMedicineName(), row.getTotalQty(), row.getTotalRevenue()));
            units += row.getTotalQty();
            revenue = revenue.add(row.getTotalRevenue() == null ? BigDecimal.ZERO : row.getTotalRevenue());
        }
        return table("Medicine Sales Report", columns, rows,
                Map.of("Units Sold", units, "Revenue (LKR)", revenue));
    }

    private Map<String, Object> topSellingReport(LocalDateTime start, LocalDateTime end) {
        List<String> columns = List.of("Rank", "Medicine", "Units Sold", "Revenue (LKR)");
        List<List<Object>> rows = new ArrayList<>();
        int rank = 1;
        for (var row : saleRepository.findTopSelling(start, end, 10)) {
            rows.add(List.of(rank++, row.getMedicineName(), row.getTotalQty(), row.getTotalRevenue()));
        }
        return table("Top Selling Medicines", columns, rows, Map.of());
    }

    private Map<String, Object> lowStockReport() {
        List<String> columns = List.of("Medicine", "Batch", "Expiry", "Available",
                "Reorder Level", "Status");
        List<List<Object>> rows = new ArrayList<>();
        for (Inventory i : inventoryRepository.findAll(Sort.by(Sort.Direction.ASC, "batch.medicine.name"))) {
            if (i.getQuantityAvailable() <= i.getReorderLevel()) {
                MedicineBatch b = i.getBatch();
                rows.add(List.of(b.getMedicine().getName(), b.getBatchNumber(),
                        b.getExpiryDate().toString(), i.getQuantityAvailable(), i.getReorderLevel(),
                        b.getExpiryDate().isBefore(LocalDate.now()) ? "EXPIRED" : "LOW STOCK"));
            }
        }
        return table("Low Stock Report", columns, rows,
                Map.of("Low Stock Batches", rows.size()));
    }

    private Map<String, Object> expiryReport() {
        List<String> columns = List.of("Medicine", "Batch", "Expiry Date", "Available",
                "Selling Price", "Status");
        List<List<Object>> rows = new ArrayList<>();
        long expired = 0;
        long near = 0;
        LocalDate today = LocalDate.now();
        for (MedicineBatch b : batchRepository.findAll(Sort.by(Sort.Direction.ASC, "expiryDate"))) {
            Integer qty = inventoryRepository.findByBatchId(b.getId())
                    .map(i -> i.getQuantityAvailable()).orElse(0);
            if (qty == null || qty <= 0) continue;
            String status;
            if (b.getExpiryDate().isBefore(today)) {
                status = "EXPIRED";
                expired++;
            } else if (b.getExpiryDate().isBefore(today.plusDays(warningDays))) {
                status = "NEAR EXPIRY";
                near++;
            } else {
                continue;
            }
            rows.add(List.of(b.getMedicine().getName(), b.getBatchNumber(),
                    b.getExpiryDate().toString(), qty, b.getSellingPrice(), status));
        }
        return table("Expiry Report", columns, rows,
                Map.of("Expired Batches", expired, "Near Expiry Batches", near));
    }

    private Map<String, Object> purchaseReport(LocalDateTime start, LocalDateTime end) {
        List<String> columns = List.of("PO Number", "Supplier", "Order Date", "Expected",
                "Items", "Total (LKR)", "Status");
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrder po : poRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"))) {
            if (po.getOrderDate() == null
                    || po.getOrderDate().isBefore(start) || !po.getOrderDate().isBefore(end)) continue;
            rows.add(List.of(po.getPoNumber(), po.getSupplier().getName(),
                    po.getOrderDate().toLocalDate().toString(),
                    po.getExpectedDate() == null ? "-" : po.getExpectedDate().toString(),
                    po.getItems().size(), po.getTotalAmount(), po.getStatus()));
            total = total.add(po.getTotalAmount() == null ? BigDecimal.ZERO : po.getTotalAmount());
        }
        return table("Purchase Report", columns, rows,
                Map.of("Purchase Orders", rows.size(), "Total Value (LKR)", total));
    }

    private Map<String, Object> supplierReport() {
        List<String> columns = List.of("Supplier", "Contact Person", "Phone", "Email",
                "Purchase Orders", "Status");
        List<List<Object>> rows = new ArrayList<>();
        for (Supplier s : supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))) {
            long poCount = poRepository.findAll().stream()
                    .filter(p -> p.getSupplier().getId().equals(s.getId()))
                    .count();
            rows.add(List.of(s.getName(), nullToDash(s.getContactPerson()), nullToDash(s.getPhone()),
                    nullToDash(s.getEmail()), poCount, s.getActive() ? "ACTIVE" : "INACTIVE"));
        }
        return table("Supplier Report", columns, rows,
                Map.of("Suppliers", rows.size()));
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> table(String title, List<String> columns,
                                      List<List<Object>> rows, Map<String, Object> totals) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("columns", columns);
        m.put("rows", rows);
        m.put("totals", totals);
        m.put("generatedAt", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return m;
    }

    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
