package com.befit.backend.dto;

import com.befit.backend.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Request bodies of the billing API. Business rules (limits, totals) are re-checked in BillCalculator. */
public final class BillRequests {
    private BillRequests() {}

    public record Item(@NotBlank(message = "Each line needs a description") @Size(max = 255) String description,
                       @NotNull(message = "Quantity is required") Integer quantity,
                       @NotNull(message = "Unit price is required") BigDecimal unitPrice) {}

    /**
     * Used for BOTH POST /api/billing/manual and POST /api/billing/appointment/{id}.
     * Old clients send only { patientName, amount, description } and keep working: that becomes a single line.
     */
    public record Create(Long patientId, @Size(max = 150) String patientName, @Size(max = 255) String description,
                         BigDecimal amount, @Valid @Size(max = 50) List<Item> items,
                         BigDecimal discountAmount, BigDecimal taxPercent,
                         PaymentMethod paymentMethod, Boolean payNow, @Size(max = 100) String referenceNo,
                         @Size(max = 100) String idempotencyKey, String branchId) {}

    public record Update(@Valid @NotNull(message = "At least one line item is required") @Size(min = 1, max = 50) List<Item> items,
                         BigDecimal discountAmount, BigDecimal taxPercent, @Size(max = 255) String description,
                         @NotBlank(message = "Please give a reason for the change") @Size(max = 500) String reason,
                         Integer expectedRevision) {}

    public record Payment(@NotNull(message = "Amount is required") BigDecimal amount,
                          @NotNull(message = "Choose a payment method") PaymentMethod method,
                          @Size(max = 100) String referenceNo, @Size(max = 100) String idempotencyKey) {}

    public record Cancel(@NotBlank(message = "Please give a reason for cancelling") @Size(max = 500) String reason) {}
}
