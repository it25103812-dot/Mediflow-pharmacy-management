package com.mediflow.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/** Renders the generic report table produced by ReportService as CSV or PDF. */
@Service
public class ExportService {

    public byte[] toCsv(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) report.get("columns");
        @SuppressWarnings("unchecked")
        List<List<Object>> rows = (List<List<Object>>) report.get("rows");
        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) report.get("totals");

        sb.append(csv(report.get("title"))).append("\n");
        sb.append(String.join(",", columns.stream().map(this::csv).toList())).append("\n");
        for (List<Object> row : rows) {
            sb.append(row.stream().map(v -> csv(v == null ? "" : String.valueOf(v)))
                    .collect(java.util.stream.Collectors.joining(","))).append("\n");
        }
        if (totals != null && !totals.isEmpty()) {
            sb.append("\n");
            totals.forEach((k, v) -> sb.append(csv(k)).append(",").append(csv(String.valueOf(v))).append("\n"));
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] toPdf(Map<String, Object> report) {
        try {
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) report.get("columns");
            @SuppressWarnings("unchecked")
            List<List<Object>> rows = (List<List<Object>>) report.get("rows");
            @SuppressWarnings("unchecked")
            Map<String, Object> totals = (Map<String, Object>) report.get("totals");

            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph header = new Paragraph("LankaCare Pharmacy (Pvt) Ltd - MediFlow", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph title = new Paragraph(String.valueOf(report.get("title")), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph generated = new Paragraph("Generated: " + report.get("generatedAt"),
                    FontFactory.getFont(FontFactory.HELVETICA, 8));
            generated.setAlignment(Element.ALIGN_CENTER);
            generated.setSpacingAfter(12);
            document.add(generated);

            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            for (String col : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(col, headFont));
                cell.setBackgroundColor(new java.awt.Color(41, 128, 185));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
            for (List<Object> row : rows) {
                for (Object v : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : String.valueOf(v), cellFont));
                    cell.setPadding(4);
                    table.addCell(cell);
                }
            }
            document.add(table);

            if (totals != null && !totals.isEmpty()) {
                document.add(new Paragraph(" ", cellFont));
                for (Map.Entry<String, Object> e : totals.entrySet()) {
                    document.add(new Paragraph(e.getKey() + ": " + e.getValue(), headFont));
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new com.mediflow.exception.ApiException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF generation failed: " + e.getMessage());
        }
    }

    private String csv(Object value) {
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
