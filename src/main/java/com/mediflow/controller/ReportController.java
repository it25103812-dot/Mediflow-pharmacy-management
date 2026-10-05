package com.mediflow.controller;

import com.mediflow.service.ExportService;
import com.mediflow.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;

    public ReportController(ReportService reportService, ExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }

    /** ?type=sales|revenue|medicine-sales|top-selling|low-stock|expiry|purchase|supplier */
    @GetMapping
    public Map<String, Object> report(@RequestParam String type,
                                      @RequestParam(required = false) LocalDate start,
                                      @RequestParam(required = false) LocalDate end) {
        return reportService.build(type, start, end);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam String type,
                                      @RequestParam(required = false) LocalDate start,
                                      @RequestParam(required = false) LocalDate end) {
        Map<String, Object> report = reportService.build(type, start, end);
        byte[] pdf = exportService.toPdf(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + type + "-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> csv(@RequestParam String type,
                                      @RequestParam(required = false) LocalDate start,
                                      @RequestParam(required = false) LocalDate end) {
        Map<String, Object> report = reportService.build(type, start, end);
        byte[] csv = exportService.toCsv(report);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + type + "-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
