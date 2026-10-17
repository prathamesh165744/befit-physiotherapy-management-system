package com.befit.backend.service;

import com.befit.backend.entity.BillStatus;
import com.befit.backend.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * All invoice arithmetic in one pure, easily-tested place. Used on create, edit and seeding, so totals can
 * never be computed two different ways.
 *
 *   lineTotal = quantity x unitPrice
 *   subtotal  = sum(lineTotal)
 *   taxable   = subtotal - discount
 *   tax       = taxable x taxPercent / 100
 *   total     = taxable + tax
 * Every amount is rounded to 2 decimals (HALF_UP).
 */
public final class BillCalculator {
    public static final int MAX_ITEMS = 50;
    public static final int MAX_QUANTITY = 1000;
    public static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("1000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private BillCalculator() {}

    public record Line(String description, int quantity, BigDecimal unitPrice) {}

    public record Result(List<BigDecimal> lineTotals, BigDecimal subtotal, BigDecimal discount,
                         BigDecimal taxPercent, BigDecimal tax, BigDecimal total) {}

    public static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    public static Result calculate(List<Line> lines, BigDecimal discount, BigDecimal taxPercent) {
        if (lines == null || lines.isEmpty()) throw ApiException.badRequest("An invoice needs at least one line item.");
        if (lines.size() > MAX_ITEMS) throw ApiException.badRequest("An invoice can have at most " + MAX_ITEMS + " line items.");

        List<BigDecimal> lineTotals = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            String row = "Line " + (i + 1) + ": ";
            if (l.description() == null || l.description().isBlank()) throw ApiException.badRequest(row + "description is required.");
            if (l.description().length() > 255) throw ApiException.badRequest(row + "description is too long.");
            if (l.quantity() < 1 || l.quantity() > MAX_QUANTITY) throw ApiException.badRequest(row + "quantity must be between 1 and " + MAX_QUANTITY + ".");
            if (l.unitPrice() == null || l.unitPrice().signum() < 0) throw ApiException.badRequest(row + "price cannot be negative.");
            if (l.unitPrice().compareTo(MAX_UNIT_PRICE) > 0) throw ApiException.badRequest(row + "price is too large.");
            BigDecimal lineTotal = money(l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())));
            lineTotals.add(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = money(subtotal);

        BigDecimal disc = money(discount);
        if (disc.signum() < 0) throw ApiException.badRequest("Discount cannot be negative.");
        if (disc.compareTo(subtotal) > 0) throw ApiException.badRequest("Discount cannot be more than the subtotal.");

        BigDecimal pct = taxPercent == null ? BigDecimal.ZERO : taxPercent.setScale(2, RoundingMode.HALF_UP);
        if (pct.signum() < 0 || pct.compareTo(HUNDRED) > 0) throw ApiException.badRequest("Tax must be between 0% and 100%.");

        BigDecimal taxable = subtotal.subtract(disc);
        BigDecimal tax = money(taxable.multiply(pct).divide(HUNDRED, 4, RoundingMode.HALF_UP));
        BigDecimal total = money(taxable.add(tax));
        if (total.signum() <= 0) throw ApiException.badRequest("The invoice total must be greater than zero.");
        return new Result(lineTotals, subtotal, disc, pct, tax, total);
    }

    /** Status follows the money: nothing paid = UNPAID, all paid = PAID, else PARTIAL. A cancelled invoice stays CANCELLED. */
    public static BillStatus statusFor(BigDecimal total, BigDecimal paid, boolean cancelled) {
        if (cancelled) return BillStatus.CANCELLED;
        BigDecimal p = money(paid);
        if (p.signum() <= 0) return BillStatus.UNPAID;
        return p.compareTo(money(total)) >= 0 ? BillStatus.PAID : BillStatus.PARTIAL;
    }
}
