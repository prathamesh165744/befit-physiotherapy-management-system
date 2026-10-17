package com.befit.backend.service;

import com.befit.backend.dto.BillDTO;
import com.befit.backend.entity.Bill;
import com.befit.backend.entity.BillStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillExcelExporterTest {

    private static BillDTO row(long id, String name, String total, String paid, String status) {
        BigDecimal t = new BigDecimal(total), p = new BigDecimal(paid);
        BigDecimal bal = "CANCELLED".equals(status) ? BigDecimal.ZERO : t.subtract(p);
        return new BillDTO(id, "INV-KARVE-ROAD-00000" + id, 100L + id, name, "KARVE-ROAD", "Karve Road, Pune", 500L + id,
                LocalDateTime.of(2026, 10, 1, 9, 30), "Session", t, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                t, p, bal, status, "CASH", 0, t);
    }

    @Test
    void producesAValidWorkbookWithTypedCells() throws Exception {
        List<BillDTO> rows = List.of(
                row(1, "Asha Rao", "1000.00", "1000.00", "PAID"),
                row(2, "=1+1", "500.50", "100.00", "PARTIAL"),          // looks like a formula; must stay text
                row(3, "Cancelled Person", "300.00", "0.00", "CANCELLED"));
        byte[] bytes = BillExcelExporter.export(rows, "Karve Road, Pune", "None", "reception@befit.com");

        assertEquals('P', bytes[0]); assertEquals('K', bytes[1]); // .xlsx is a zip file ("PK")
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet s = wb.getSheet("Billing");
            assertNotNull(s);
            Row h = s.getRow(0);
            assertEquals("Invoice ID", h.getCell(0).getStringCellValue());
            assertEquals("Outstanding Balance", h.getCell(12).getStringCellValue());
            assertEquals("Payment Status", h.getCell(14).getStringCellValue());

            Row r1 = s.getRow(1);
            assertEquals(CellType.NUMERIC, r1.getCell(0).getCellType());
            assertEquals(CellType.NUMERIC, r1.getCell(10).getCellType());
            assertEquals(1000.0, r1.getCell(10).getNumericCellValue(), 0.0001);
            assertTrue(DateUtil.isCellDateFormatted(r1.getCell(6)), "invoice date must be a real date cell");

            Row r2 = s.getRow(2);
            assertEquals(CellType.STRING, r2.getCell(2).getCellType());
            assertEquals("=1+1", r2.getCell(2).getStringCellValue());
            assertEquals(400.50, r2.getCell(12).getNumericCellValue(), 0.0001);

            // totals row excludes the CANCELLED invoice: 1000 + 500.50 = 1500.50
            Row totals = s.getRow(4);
            assertEquals(CellType.FORMULA, totals.getCell(10).getCellType());
            assertEquals(1500.50, totals.getCell(10).getNumericCellValue(), 0.0001);
            assertEquals(1100.00, totals.getCell(11).getNumericCellValue(), 0.0001); // paid

            assertNotNull(wb.getSheet("Report Info"));
        }
    }

    @Test
    void emptyResultStillProducesAValidFile() throws Exception {
        byte[] bytes = BillExcelExporter.export(List.of(), "All branches", "None", "admin@befit.com");
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Invoice ID", wb.getSheet("Billing").getRow(0).getCell(0).getStringCellValue());
            assertNull(wb.getSheet("Billing").getRow(1));
        }
    }

    @Test
    void legacyBillWithoutNewColumnsMapsSafely() {
        Bill b = new Bill();
        b.setAmount(600.0);
        b.setStatus(BillStatus.PAID);
        BillDTO d = BillDTO.of(b, "Karve Road");
        assertEquals(new BigDecimal("600.00"), d.totalAmount());
        assertEquals(new BigDecimal("600.00"), d.amountPaid());
        assertEquals(new BigDecimal("0.00"), d.balanceDue());
        assertEquals("PAID", d.status());
    }

    @Test
    void cancelledBillHasNoOutstandingBalance() {
        Bill b = new Bill();
        b.setTotalAmount(new BigDecimal("900"));
        b.setAmountPaid(new BigDecimal("0"));
        b.setStatus(BillStatus.CANCELLED);
        assertEquals(new BigDecimal("0.00"), BillDTO.of(b, "x").balanceDue());
    }
}
