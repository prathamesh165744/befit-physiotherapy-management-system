package com.befit.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Data for the "Print All" billing report: every invoice matching the filters, across all pages, plus totals. */
public record BillReportDTO(String branchLabel, String filterSummary, LocalDateTime generatedAt, String generatedBy,
                            List<BillDTO> rows, Totals totals, boolean truncated, int maxRows) {
    public record Totals(int count, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total,
                         BigDecimal paid, BigDecimal outstanding) {}
}
