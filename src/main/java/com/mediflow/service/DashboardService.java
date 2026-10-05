package com.mediflow.service;

import com.mediflow.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every number on the dashboards is computed from live database queries. */
@Service
public class DashboardService {

    private final SaleRepository saleRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final MedicineBatchRepository batchRepository;
    private final PurchaseOrderRepository poRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final GrnService grnService;
    private final AuditLogRepository auditLogRepository;
    private final SaleService saleService;

    @org.springframework.beans.factory.annotation.Value("${mediflow.expiry.warning-days:90}")
    private int warningDays;

    public DashboardService(SaleRepository saleRepository,
                            MedicineRepository medicineRepository,
                            InventoryRepository inventoryRepository,
                            MedicineBatchRepository batchRepository,
                            PurchaseOrderRepository poRepository,
                            CustomerRepository customerRepository,
                            UserRepository userRepository,
                            GrnService grnService,
                            AuditLogRepository auditLogRepository,
                            SaleService saleService) {
        this.saleRepository = saleRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.poRepository = poRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.grnService = grnService;
        this.auditLogRepository = auditLogRepository;
        this.saleService = saleService;
    }

    public Map<String, Object> summary() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        Map<String, Object> m = new HashMap<>();
        m.put("todaySales", saleRepository.countBySaleDateBetween(dayStart, dayEnd));
        m.put("todayRevenue", nvl(saleRepository.sumTotalBetween(dayStart, dayEnd)));
        m.put("totalRevenue", nvl(saleRepository.sumAllTimeRevenue()));
        m.put("totalMedicines", medicineRepository.count());
        m.put("totalStock", inventoryRepository.sumTotalStock() == null ? 0 : inventoryRepository.sumTotalStock());
        m.put("lowStockCount", inventoryRepository.countLowStock());
        m.put("nearExpiryCount", batchRepository.countByExpiryDateBetween(today, today.plusDays(warningDays)));
        m.put("expiredCount", batchRepository.countByExpiryDateBefore(today));
        m.put("pendingPurchaseOrders", poRepository.countByStatusIn(
                List.of("DRAFT", "SENT", "PARTIALLY_RECEIVED")));
        m.put("pendingGrn", grnService.countPending());
        m.put("totalCustomers", customerRepository.count());
        m.put("totalUsers", userRepository.count());
        return m;
    }

    public List<Map<String, Object>> dailySales(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("label", d.getDayOfMonth() + "/" + d.getMonthValue());
            row.put("orders", saleRepository.countBySaleDateBetween(start, end));
            row.put("revenue", nvl(saleRepository.sumTotalBetween(start, end)));
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> monthlyRevenue(int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.toString());
            row.put("label", ym.getMonth().name().substring(0, 3) + " " + ym.getYear());
            row.put("revenue", nvl(saleRepository.sumTotalBetween(start, end)));
            row.put("orders", saleRepository.countBySaleDateBetween(start, end));
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> topSelling(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        // Wide bounds instead of NULL params so the native query behaves the same on H2 and MySQL
        LocalDateTime from = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.now().plusYears(5);
        for (var row : saleRepository.findTopSelling(from, to, limit)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("medicineId", row.getMedicineId());
            m.put("medicineName", row.getMedicineName());
            m.put("totalQty", row.getTotalQty());
            m.put("totalRevenue", row.getTotalRevenue());
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> recentActivity(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var a : auditLogRepository
                .findAll(org.springframework.data.domain.PageRequest.of(0, limit,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("user", a.getUser() != null ? a.getUser().getFullName() : "System");
            m.put("action", a.getAction());
            m.put("entityType", a.getEntityType());
            m.put("entityId", a.getEntityId());
            m.put("details", a.getDetails());
            m.put("createdAt", a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
            result.add(m);
        }
        return result;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
