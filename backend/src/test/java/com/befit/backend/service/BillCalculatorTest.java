package com.befit.backend.service;

import com.befit.backend.entity.BillStatus;
import com.befit.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillCalculatorTest {

    private static BillCalculator.Line line(String d, int q, String price) {
        return new BillCalculator.Line(d, q, new BigDecimal(price));
    }

    @Test
    void totalsFollowTheDocumentedFormula() {
        // 2 x 800 + 1 x 350 = 1950 ; minus 50 discount = 1900 ; 5% tax = 95 ; total 1995
        var r = BillCalculator.calculate(List.of(line("Session", 2, "800"), line("Band", 1, "350")),
                new BigDecimal("50"), new BigDecimal("5"));
        assertEquals(new BigDecimal("1950.00"), r.subtotal());
        assertEquals(new BigDecimal("50.00"), r.discount());
        assertEquals(new BigDecimal("95.00"), r.tax());
        assertEquals(new BigDecimal("1995.00"), r.total());
        assertEquals(List.of(new BigDecimal("1600.00"), new BigDecimal("350.00")), r.lineTotals());
    }

    @Test
    void noDiscountNoTaxMeansTotalEqualsSubtotal() {
        var r = BillCalculator.calculate(List.of(line("Session", 1, "500")), null, null);
        assertEquals(new BigDecimal("500.00"), r.total());
        assertEquals(new BigDecimal("0.00"), r.tax());
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        var r = BillCalculator.calculate(List.of(line("Odd price", 3, "33.335")), null, null);
        assertEquals(new BigDecimal("100.01"), r.total()); // 100.005 -> 100.01
    }

    @Test
    void rejectsBadInput() {
        assertEquals(400, assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(), null, null)).getStatus());
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line("x", 0, "10")), null, null));
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line("x", 1, "-1")), null, null));
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line(" ", 1, "10")), null, null));
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line("x", 1, "10")), new BigDecimal("11"), null)); // discount > subtotal
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line("x", 1, "10")), new BigDecimal("-1"), null));
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line("x", 1, "10")), null, new BigDecimal("101")));
        assertThrows(ApiException.class, () -> BillCalculator.calculate(List.of(line("x", 1, "10")), new BigDecimal("10"), null)); // total would be 0
    }

    @Test
    void statusFollowsTheMoney() {
        BigDecimal total = new BigDecimal("1000");
        assertEquals(BillStatus.UNPAID, BillCalculator.statusFor(total, BigDecimal.ZERO, false));
        assertEquals(BillStatus.PARTIAL, BillCalculator.statusFor(total, new BigDecimal("400"), false));
        assertEquals(BillStatus.PAID, BillCalculator.statusFor(total, new BigDecimal("1000.00"), false));
        assertEquals(BillStatus.CANCELLED, BillCalculator.statusFor(total, new BigDecimal("1000"), true));
    }
}
