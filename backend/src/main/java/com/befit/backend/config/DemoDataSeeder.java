package com.befit.backend.config;

import com.befit.backend.entity.*;
import com.befit.backend.repository.*;
import com.befit.backend.service.BillCalculator;
import com.befit.backend.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Step 4 of 4 on startup: DEMO DATA for several branches, so multi-branch behaviour can be tried immediately.
 *
 *  - runs once: it does nothing if rohan@befit.com already exists, and is all-or-nothing (one transaction)
 *  - switch off with  befit.seed.demo-data=false  (or env SEED_DEMO_DATA=false)  - do this in production
 *
 * LOGINS (all demo passwords are listed in README.md):
 *   reception.baner@befit.com / reception.viman@befit.com / reception.kothrud@befit.com   password: staff123
 *   dr.himanshu@ / dr.smita@ / dr.rohan@ / dr.anjali@befit.com                             password: doctor123
 *   rohan@befit.com   patient123  -> moved Karve Road -> Baner; old visits/invoices stay at Karve Road
 *   meera@befit.com   patient123  -> at Viman Nagar, has a PENDING request to move to Kothrud
 *   anita@befit.com   patient123  -> plain patient at Karve Road
 */
@Component
@Order(4)
@ConditionalOnProperty(name = "befit.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String MARKER_EMAIL = "rohan@befit.com";

    private static final String KARVE = "KARVE-ROAD", BANER = "BANER", VIMAN = "VIMAN-NAGAR", KOTHRUD = "KOTHRUD";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientBranchHistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final BranchTransferRequestRepository transferRepository;
    private final BillingService billingService;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, List<User>> doctorsByBranch = new HashMap<>();
    private int phoneCounter = 1000;

    public DemoDataSeeder(UserRepository userRepository, PatientRepository patientRepository,
                          PatientBranchHistoryRepository historyRepository, AppointmentRepository appointmentRepository,
                          BranchTransferRequestRepository transferRepository, BillingService billingService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.historyRepository = historyRepository;
        this.appointmentRepository = appointmentRepository;
        this.transferRepository = transferRepository;
        this.billingService = billingService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(MARKER_EMAIL)) return;

        // ---------- staff for every branch
        User karveDoc1 = userRepository.findByEmail("doctor@befit.com").orElseGet(() -> staff("doctor@befit.com", "Dr. Sarah Jenkins", Role.DOCTOR, KARVE, "doctor123"));
        User karveDoc2 = staff("dr.himanshu@befit.com", "Dr. Himanshu Jain", Role.DOCTOR, KARVE, "doctor123");
        doctorsByBranch.put(KARVE, List.of(karveDoc1, karveDoc2));
        doctorsByBranch.put(BANER, List.of(staff("dr.smita@befit.com", "Dr. Smita Patil", Role.DOCTOR, BANER, "doctor123")));
        doctorsByBranch.put(VIMAN, List.of(staff("dr.rohan@befit.com", "Dr. Rohan Shah", Role.DOCTOR, VIMAN, "doctor123")));
        doctorsByBranch.put(KOTHRUD, List.of(staff("dr.anjali@befit.com", "Dr. Anjali Kulkarni", Role.DOCTOR, KOTHRUD, "doctor123")));
        staff("reception.baner@befit.com", "Front Desk - Baner", Role.RECEPTIONIST, BANER, "staff123");
        staff("reception.viman@befit.com", "Front Desk - Viman Nagar", Role.RECEPTIONIST, VIMAN, "staff123");
        staff("reception.kothrud@befit.com", "Front Desk - Kothrud", Role.RECEPTIONIST, KOTHRUD, "staff123");

        // ---------- ordinary patients: enough invoices at Karve Road (24) to see pagination at 10 per page
        seedBranchPatients(KARVE, List.of("Aarav Joshi", "Sneha Patil", "Vikram Kulkarni", "Neha Deshpande", "Rahul Pawar", "Pooja Bhosale",
                "Amit Jadhav", "Kavita More", "Sanjay Gokhale", "Ritu Sharma", "Manish Kale", "Divya Nair"));
        seedBranchPatients(BANER, List.of("Tanvi Chavan", "Omkar Shinde", "Prachi Gaikwad", "Nikhil Wagh", "Shruti Desai", "Yash Mehta"));
        seedBranchPatients(VIMAN, List.of("Isha Kapoor", "Harsh Agarwal", "Swati Rane", "Karan Bhatia", "Aditi Menon", "Varun Iyer"));
        seedBranchPatients(KOTHRUD, List.of("Sakshi Thorat", "Pranav Joshi", "Ankita Sawant", "Rajesh Naik", "Madhuri Kelkar", "Siddharth Rao"));

        seedTransferScenario();
        seedPendingTransferScenario();
        patientWithLogin("anita@befit.com", "Anita Rao", KARVE, "Female", LocalDateTime.now().minusDays(5));

        log.info("Demo data created for branches KARVE-ROAD, BANER, VIMAN-NAGAR, KOTHRUD (see README for logins)");
    }

    // ===================================================================== scenarios

    /** Rohan: 3 visits + 3 invoices at Karve Road, then moved to Baner (approved). New visit is at Baner. */
    private void seedTransferScenario() {
        LocalDateTime now = LocalDateTime.now();
        Patient rohan = patientWithLogin(MARKER_EMAIL, "Rohan Deshmukh", KARVE, "Male", now.minusDays(40));
        User karveDoc = doctorsByBranch.get(KARVE).get(0);

        Appointment a1 = appointment(rohan, karveDoc, KARVE, now.minusDays(35).with(LocalTime.of(10, 0)), AppointmentStatus.COMPLETED, "Lower Back Pain");
        Appointment a2 = appointment(rohan, karveDoc, KARVE, now.minusDays(28).with(LocalTime.of(10, 0)), AppointmentStatus.COMPLETED, "Lower Back Pain");
        Appointment a3 = appointment(rohan, karveDoc, KARVE, now.minusDays(21).with(LocalTime.of(10, 0)), AppointmentStatus.COMPLETED, "Lower Back Pain");

        Bill b1 = bill(KARVE, rohan, a1, now.minusDays(35), 1, "Physiotherapy Session", 800, 0, 0);
        billingService.recordPayment(b1, b1.getTotalAmount(), PaymentMethod.CASH, null, null, "reception@befit.com", now.minusDays(35));
        Bill b2 = bill(KARVE, rohan, a2, now.minusDays(28), 1, "Physiotherapy Session", 800, 0, 0);
        billingService.recordPayment(b2, b2.getTotalAmount(), PaymentMethod.UPI, "UPI-REF-7788", null, "reception@befit.com", now.minusDays(28));
        Bill b3 = bill(KARVE, rohan, a3, now.minusDays(21), 2, "Physiotherapy Session", 1000, 0, 0); // 2000, half paid
        billingService.recordPayment(b3, new BigDecimal("1000.00"), PaymentMethod.CARD, "CARD-4421", null, "reception@befit.com", now.minusDays(21));

        // approved transfer Karve Road -> Baner 10 days ago
        LocalDateTime moved = now.minusDays(10);
        PatientBranchHistory first = historyRepository.findByPatientIdAndToDateIsNull(rohan.getId()).orElseThrow();
        first.setToDate(moved);
        historyRepository.save(first);
        PatientBranchHistory second = new PatientBranchHistory(rohan.getId(), BANER);
        second.setFromDate(moved);
        second.setAuthorizedBy("reception.baner@befit.com");
        second.setReason("Moved closer to the new office");
        historyRepository.save(second);
        rohan.setBranchId(BANER);
        rohan.getUser().setBranchId(BANER);
        patientRepository.save(rohan);

        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(rohan.getId());
        t.setFromBranchId(KARVE);
        t.setToBranchId(BANER);
        t.setStatus(TransferStatus.APPROVED);
        t.setReason("Moved closer to the new office");
        t.setRequestedBy(MARKER_EMAIL);
        t.setRequestedAt(moved.minusDays(1));
        t.setDecidedBy("reception.baner@befit.com");
        t.setDecidedAt(moved);
        t.setDecisionNote("Welcome to Baner");
        transferRepository.save(t);

        // first visit at the NEW branch - today, waiting
        appointment(rohan, doctorsByBranch.get(BANER).get(0), BANER, now.with(LocalTime.of(12, 0)), AppointmentStatus.WAITING, "Lower Back Pain");
    }

    /** Meera: at Viman Nagar with a PENDING request to Kothrud - shows up in the Kothrud receptionist's Transfers page. */
    private void seedPendingTransferScenario() {
        LocalDateTime now = LocalDateTime.now();
        Patient meera = patientWithLogin("meera@befit.com", "Meera Kulkarni", VIMAN, "Female", now.minusDays(20));
        Appointment a = appointment(meera, doctorsByBranch.get(VIMAN).get(0), VIMAN, now.minusDays(12).with(LocalTime.of(11, 0)), AppointmentStatus.COMPLETED, "Neck Pain");
        Bill b = bill(VIMAN, meera, a, now.minusDays(12), 1, "Physiotherapy Session", 900, 0, 0);
        billingService.recordPayment(b, b.getTotalAmount(), PaymentMethod.UPI, "UPI-REF-5521", null, "reception.viman@befit.com", now.minusDays(12));

        BranchTransferRequest t = new BranchTransferRequest();
        t.setPatientId(meera.getId());
        t.setFromBranchId(VIMAN);
        t.setToBranchId(KOTHRUD);
        t.setStatus(TransferStatus.PENDING);
        t.setReason("I have started working near Kothrud");
        t.setRequestedBy("meera@befit.com");
        t.setRequestedAt(now.minusHours(3));
        transferRepository.save(t);
    }

    /** A few patients per branch, each with two completed visits whose invoices cover every payment status. */
    private void seedBranchPatients(String branchId, List<String> names) {
        LocalDateTime now = LocalDateTime.now();
        List<User> docs = doctorsByBranch.get(branchId);
        String reception = branchId.equals(KARVE) ? "reception@befit.com" : "reception." + (branchId.equals(VIMAN) ? "viman" : branchId.toLowerCase()) + "@befit.com";
        int i = 0;
        for (String name : names) {
            Patient p = patient(name, branchId, i % 2 == 0 ? "Male" : "Female", now.minusDays(60 + i));
            User doc = docs.get(i % docs.size());
            for (int v = 0; v < 2; v++) {
                int idx = i * 2 + v;
                LocalDateTime when = now.minusDays(3 + idx).with(LocalTime.of(9 + (idx % 6), 0));
                Appointment a = appointment(p, doc, branchId, when, AppointmentStatus.COMPLETED, v == 0 ? "Consultation" : "Sports Injury");
                // vary amounts, discount and tax so the billing screens show different shapes of invoices
                Bill b = bill(branchId, p, a, when, 1 + (idx % 2), idx % 3 == 0 ? "Physiotherapy Session" : "Therapeutic Exercise Session",
                        600 + (idx % 5) * 100, idx % 4 == 0 ? 50 : 0, idx % 5 == 0 ? 5 : 0);
                switch (idx % 4) {
                    case 0 -> { /* stays UNPAID */ }
                    case 1 -> billingService.recordPayment(b, b.getTotalAmount(), PaymentMethod.CASH, null, null, reception, when);
                    case 2 -> billingService.recordPayment(b, b.getTotalAmount(), PaymentMethod.UPI, "UPI-" + (9000 + idx), null, reception, when);
                    default -> billingService.recordPayment(b, b.getTotalAmount().divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP),
                            PaymentMethod.CARD, "CARD-" + (4000 + idx), null, reception, when);
                }
            }
            // today's queue so the receptionist dashboard is not empty (the 2nd patient is already checked in)
            if (i < 3) {
                appointment(p, doc, branchId, now.with(LocalTime.of(14 + i, 0)),
                        i == 1 ? AppointmentStatus.CHECKED_IN : AppointmentStatus.WAITING, "Follow-up");
            }
            i++;
        }
    }

    // ===================================================================== helpers

    private User staff(String email, String name, Role role, String branchId, String password) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) return existing.get();
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(role);
        u.setBranchId(branchId);
        u.setPhone("98200" + (phoneCounter++));
        u.setActive(true);
        return userRepository.save(u);
    }

    private Patient patientWithLogin(String email, String name, String branchId, String gender, LocalDateTime registered) {
        User login = new User();
        login.setEmail(email);
        login.setFullName(name);
        login.setPassword(passwordEncoder.encode("patient123"));
        login.setRole(Role.PATIENT);
        login.setBranchId(branchId);
        login.setPhone("98760" + (phoneCounter++));
        login.setActive(true);
        login = userRepository.save(login);

        Patient p = patient(name, branchId, gender, registered);
        p.setUser(login);
        return patientRepository.save(p);
    }

    private Patient patient(String name, String branchId, String gender, LocalDateTime registered) {
        Patient p = new Patient();
        p.setFullName(name);
        p.setPhone("98765" + (phoneCounter++));
        p.setGender(gender);
        p.setDateOfBirth(LocalDate.of(1975 + (phoneCounter % 30), 1 + (phoneCounter % 12), 1 + (phoneCounter % 27)));
        p.setAddress("Pune, Maharashtra");
        p.setEmergencyContactName("Family member of " + name.split(" ")[0]);
        p.setEmergencyContact("97000" + (phoneCounter++));
        p.setBranchId(branchId);
        p.setRegistrationDate(registered.toLocalDate());
        p = patientRepository.save(p);

        PatientBranchHistory h = new PatientBranchHistory(p.getId(), branchId);
        h.setFromDate(registered);
        h.setReason("Registered at branch");
        historyRepository.save(h);
        return p;
    }

    private Appointment appointment(Patient p, User doctor, String branchId, LocalDateTime when, AppointmentStatus status, String caseType) {
        Appointment a = new Appointment();
        a.setPatient(p);
        a.setDoctorId(doctor.getId());
        a.setDoctorName(doctor.getFullName());
        a.setAppointmentDate(when);
        a.setBranchId(branchId);
        a.setStatus(status);
        a.setCaseType(caseType);
        if (status == AppointmentStatus.CHECKED_IN || status == AppointmentStatus.COMPLETED) a.setCheckInTime(when);
        return appointmentRepository.save(a);
    }

    private Bill bill(String branchId, Patient p, Appointment a, LocalDateTime when, int qty, String service, int unitPrice, int discount, int taxPercent) {
        return billingService.issueBill(branchId, p.getId(), p.getFullName(), a.getId(), service + " - " + a.getCaseType(),
                List.of(new BillCalculator.Line(service, qty, BigDecimal.valueOf(unitPrice))),
                BigDecimal.valueOf(discount), BigDecimal.valueOf(taxPercent), when, "system");
    }
}
