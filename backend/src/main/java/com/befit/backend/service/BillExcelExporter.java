package com.befit.backend.service;

import com.befit.backend.dto.BillDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Builds the .xlsx file. Numbers are written as numeric cells and dates as real date cells (with number formats),
 * so Excel can sum, sort and filter them. Text is always written as text, so a patient name that starts with
 * "=" is shown literally and can never run as a formula.
 */
public final class BillExcelExporter {
    private BillExcelExporter() {}

    static final String[] HEADERS = {
            "Invoice ID", "Invoice Number", "Patient Name", "Patient ID", "Branch", "Appointment ID", "Invoice Date",
            "Subtotal", "Discount", "Tax", "Total Amount", "Amount Paid", "Outstanding Balance", "Payment Method", "Payment Status"
    };

    public static byte[] export(List<BillDTO> rows, String branchLabel, String filterSummary, String generatedBy) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CreationHelper helper = wb.getCreationHelper();
            DataFormat fmt = wb.createDataFormat();

            Font bold = wb.createFont();
            bold.setBold(true);

            CellStyle header = wb.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle money = wb.createCellStyle();
            money.setDataFormat(fmt.getFormat("#,##0.00"));
            CellStyle moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(fmt.getFormat("#,##0.00"));
            moneyBold.setFont(bold);
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(fmt.getFormat("yyyy-mm-dd hh:mm"));
            CellStyle whole = wb.createCellStyle();
            whole.setDataFormat(fmt.getFormat("0"));
            CellStyle boldOnly = wb.createCellStyle();
            boldOnly.setFont(bold);

            Sheet sheet = wb.createSheet("Billing");
            Row h = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(header);
            }

            int r = 1;
            for (BillDTO b : rows) {
                Row row = sheet.createRow(r++);
                num(row, 0, b.id(), whole);
                row.createCell(1).setCellValue(nz(b.invoiceNumber()));
                row.createCell(2).setCellValue(nz(b.patientName()));
                num(row, 3, b.patientId(), whole);
                row.createCell(4).setCellValue(nz(b.branchName() != null ? b.branchName() : b.branchId()));
                num(row, 5, b.appointmentId(), whole);
                if (b.billDate() != null) {
                    Cell dc = row.createCell(6);
                    dc.setCellValue(toDate(b.billDate()));
                    dc.setCellStyle(date);
                }
                dec(row, 7, b.subtotal(), money);
                dec(row, 8, b.discountAmount(), money);
                dec(row, 9, b.taxAmount(), money);
                dec(row, 10, b.totalAmount(), money);
                dec(row, 11, b.amountPaid(), money);
                dec(row, 12, b.balanceDue(), money);
                row.createCell(13).setCellValue(nz(b.paymentMethod()));
                row.createCell(14).setCellValue(nz(b.status()));
            }

            // Totals row: cancelled invoices are listed but not counted.
            int last = r; // 1-based number of the last data row
            if (!rows.isEmpty()) {
                Row t = sheet.createRow(r);
                Cell label = t.createCell(6);
                label.setCellValue("TOTAL (excl. cancelled)");
                label.setCellStyle(boldOnly);
                for (int col = 7; col <= 12; col++) {
                    String letter = String.valueOf((char) ('A' + col));
                    Cell c = t.createCell(col);
                    c.setCellFormula("SUMIF($O$2:$O$" + last + ",\"<>CANCELLED\"," + letter + "$2:" + letter + "$" + last + ")");
                    c.setCellStyle(moneyBold);
                }
                sheet.setAutoFilter(new CellRangeAddress(0, last - 1, 0, HEADERS.length - 1));
            }
            sheet.createFreezePane(0, 1);
            int[] widths = {11, 30, 26, 11, 22, 15, 18, 13, 13, 13, 15, 15, 20, 17, 15};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            Sheet info = wb.createSheet("Report Info");
            String[][] meta = {
                    {"Report", "BeFit Physiotherapy - Billing export"},
                    {"Branch", nz(branchLabel)},
                    {"Filters", nz(filterSummary)},
                    {"Generated at", LocalDateTime.now().toString().replace('T', ' ')},
                    {"Generated by", nz(generatedBy)},
                    {"Rows", String.valueOf(rows.size())}
            };
            for (int i = 0; i < meta.length; i++) {
                Row ir = info.createRow(i);
                Cell k = ir.createCell(0);
                k.setCellValue(meta[i][0]);
                k.setCellStyle(boldOnly);
                ir.createCell(1).setCellValue(meta[i][1]);
            }
            info.setColumnWidth(0, 16 * 256);
            info.setColumnWidth(1, 70 * 256);

            helper.createFormulaEvaluator().evaluateAll(); // store computed totals so any viewer shows them
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void num(Row row, int col, Long v, CellStyle style) {
        if (v == null) return;
        Cell c = row.createCell(col);
        c.setCellValue(v.doubleValue());
        c.setCellStyle(style);
    }

    private static void dec(Row row, int col, BigDecimal v, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue((v == null ? BigDecimal.ZERO : v).doubleValue());
        c.setCellStyle(style);
    }

    private static Date toDate(LocalDateTime t) {
        return Date.from(t.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
