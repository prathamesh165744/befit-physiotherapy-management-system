package com.befit.backend.config;

import com.befit.backend.entity.*;
import com.befit.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Step 2 of 4 on startup: upgrades rows that existed before this version. Additive and idempotent -
 * nothing is deleted, and when there is nothing to fix every step is a cheap indexed query that returns 0 rows
 * (the old version loaded every table into memory on each start).
 */
@Component
@Order(2)
public class DataFixer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataFixer.class);
    static final String DEFAULT_BRANCH = "KARVE-ROAD";
    private static final int BATCH = 200;

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final BillItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PatientBranchHistoryRepository historyRepository;

    public DataFixer(PatientRepository patientRepository, AppointmentRepository appointmentRepository,
                     BillRepository billRepository, BillItemRepository itemRepository, PaymentRepository paymentRepository,
                     UserRepository userRepository, PatientBranchHistoryRepository historyRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int fixed = 0;

        // Doctors / receptionists without a branch work at the default branch. Admins stay null (= every branch).
        for (User u : userRepository.findByBranchIdIsNullAndRoleIn(EnumSet.of(Role.DOCTOR, Role.RECEPTIONIST))) {
            u.setBranchId(DEFAULT_BRANCH);
            userRepository.save(u);
            fixed++;
        }

        fixed += patientRepository.fillNullBranch(DEFAULT_BRANCH);
        fixed += appointmentRepository.fillNullBranch(DEFAULT_BRANCH);
        fixed += billRepository.fillNullBranch(DEFAULT_BRANCH);

        for (Patient p : patientRepository.findWithoutCurrentBranchHistory()) {
            historyRepository.save(new PatientBranchHistory(p.getId(), p.getBranchId()));
            fixed++;
        }

        for (Bill b : billRepository.findByPatientIdIsNullAndAppointmentIdIsNotNull()) {
            appointmentRepository.findById(b.getAppointmentId()).ifPresent(a -> {
                if (a.getPatient() != null) b.setPatientId(a.getPatient().getId());
            });
            billRepository.save(b);
            fixed++;
        }

        // Old invoices only had a single double "amount". Convert them into subtotal/total/paid + one line item + one payment,
        // so every invoice (old or new) follows the same rules and totals stay consistent.
        List<Bill> batch;
        while (!(batch = billRepository.findByTotalAmountIsNull(PageRequest.of(0, BATCH))).isEmpty()) {
            for (Bill b : batch) { upgradeLegacyBill(b); fixed++; }
        }

        while (!(batch = billRepository.findByInvoiceNumberIsNull(PageRequest.of(0, BATCH))).isEmpty()) {
            for (Bill b : batch) {
                b.setInvoiceNumber("INV-" + b.getBranchId() + "-" + String.format("%06d", b.getId()));
                billRepository.save(b);
                fixed++;
            }
        }

        if (fixed > 0) log.info("Multi-branch data upgrade: {} row(s) updated", fixed);
    }

    private void upgradeLegacyBill(Bill b) {
        BigDecimal total = (b.getAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(b.getAmount())).setScale(2, RoundingMode.HALF_UP);
        boolean wasPaid = b.getStatus() == null || b.getStatus() == BillStatus.PAID;
        if (b.getStatus() == null) b.setStatus(BillStatus.PAID);

        b.setSubtotal(total);
        b.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        b.setTaxPercent(BigDecimal.ZERO.setScale(2));
        b.setTaxAmount(BigDecimal.ZERO.setScale(2));
        b.setTotalAmount(total);
        b.setAmountPaid(wasPaid ? total : BigDecimal.ZERO.setScale(2));
        b.setRevision(0);
        if (b.getBillDate() == null) b.setBillDate(LocalDateTime.now());
        if (wasPaid && total.signum() > 0) b.setPaymentMethod(PaymentMethod.OTHER.name());
        billRepository.save(b);

        if (itemRepository.findByBillIdOrderByPositionAsc(b.getId()).isEmpty()) {
            String d = b.getDescription() == null || b.getDescription().isBlank() ? "Service charges" : b.getDescription();
            itemRepository.save(new BillItem(b, d, 1, total, total, 0));
        }
        if (wasPaid && total.signum() > 0 && paymentRepository.findByBillIdOrderByPaymentDateAscIdAsc(b.getId()).isEmpty()) {
            Payment p = new Payment();
            p.setBill(b);
            p.setBranchId(b.getBranchId());
            p.setAmount(total);
            p.setMethod(PaymentMethod.OTHER);   // the old system never recorded how it was paid
            p.setStatus(PaymentStatus.COMPLETED);
            p.setPaymentDate(b.getBillDate());
            p.setReferenceNo("MIGRATED");
            p.setIdempotencyKey("legacy-bill-" + b.getId());
            p.setReceivedBy("system");
            paymentRepository.save(p);
        }
    }
}
