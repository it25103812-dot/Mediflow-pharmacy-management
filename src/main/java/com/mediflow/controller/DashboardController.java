package com.mediflow.controller;

import com.mediflow.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }

    @GetMapping("/daily-sales")
    public List<Map<String, Object>> dailySales(@RequestParam(defaultValue = "14") int days) {
        return dashboardService.dailySales(days);
    }

    @GetMapping("/monthly-revenue")
    public List<Map<String, Object>> monthlyRevenue(@RequestParam(defaultValue = "12") int months) {
        return dashboardService.monthlyRevenue(months);
    }

    @GetMapping("/top-selling")
    public List<Map<String, Object>> topSelling(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.topSelling(limit);
    }

    @GetMapping("/recent-activity")
    public List<Map<String, Object>> recentActivity(@RequestParam(defaultValue = "8") int limit) {
        return dashboardService.recentActivity(limit);
    }
}
