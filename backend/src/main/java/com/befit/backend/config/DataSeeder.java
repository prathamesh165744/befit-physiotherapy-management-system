package com.befit.backend.config;

import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.repository.BranchRepository;
import com.befit.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Step 3 of 4 on startup: the three original test staff accounts (only if no admin exists yet). */
@Component
@Order(3)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed data if no Admin exists
        if (userRepository.findByEmail("admin@befit.com").isEmpty()) {
            
            // 1. Create Admin
            User admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail("admin@befit.com");
            admin.setPhone("1111111111");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);

            // 2. Create Doctor
            User doctor = new User();
            doctor.setFullName("Dr. Sarah Jenkins");
            doctor.setEmail("doctor@befit.com");
            doctor.setPhone("2222222222");
            doctor.setPassword(passwordEncoder.encode("doctor123"));
            doctor.setRole(Role.DOCTOR);
            doctor.setBranchId("KARVE-ROAD");
            doctor.setActive(true);
            userRepository.save(doctor);

            // 3. Create Receptionist
            User receptionist = new User();
            receptionist.setFullName("Front Desk");
            receptionist.setEmail("reception@befit.com");
            receptionist.setPhone("3333333333");
            receptionist.setPassword(passwordEncoder.encode("staff123"));
            receptionist.setRole(Role.RECEPTIONIST);
            receptionist.setBranchId("KARVE-ROAD");
            receptionist.setActive(true);
            userRepository.save(receptionist);

                       // 4. Extra receptionist for the Baner branch
            User baner = new User();
            baner.setFullName("Front Desk Baner");
            baner.setEmail("staff@befit.com");
            baner.setPhone("3333333333");
            baner.setPassword(passwordEncoder.encode("staff123"));
            baner.setRole(Role.RECEPTIONIST);
            baner.setBranchId("BANER");
            baner.setActive(true);
            userRepository.save(baner);

            System.out.println("✅ Test Staff Accounts Seeded Successfully!");
        } else {
            System.out.println("✅ Staff Accounts Already Exist. Skipping Seeder.");
        }
        try {
            seedBranchManagers();
        } catch (RuntimeException e) {
            // e.g. an old database whose users.role constraint does not know BRANCH_MANAGER yet - never block start-up for demo data
            System.out.println("⚠️ Could not seed branch managers: " + e.getMessage());
        }
    }

    /**
     * One demo BRANCH_MANAGER per branch that exists (idempotent, also on databases that already had staff).
     * Each manager is tied to exactly one branch. Demo password: manager123 - change it before real use.
     */
    private void seedBranchManagers() {
        manager("manager.karve@befit.com", "Manager - Karve Road", "KARVE-ROAD", "4444444441");
        manager("manager.baner@befit.com", "Manager - Baner", "BANER", "4444444442");
        manager("manager.viman@befit.com", "Manager - Viman Nagar", "VIMAN-NAGAR", "4444444443");
        manager("manager.kothrud@befit.com", "Manager - Kothrud", "KOTHRUD", "4444444444");
    }

    private void manager(String email, String name, String branchId, String phone) {
        if (!branchRepository.existsById(branchId) || userRepository.findByEmail(email).isPresent()) return;
        User m = new User();
        m.setFullName(name);
        m.setEmail(email);
        m.setPhone(phone);
        m.setPassword(passwordEncoder.encode("manager123"));
        m.setRole(Role.BRANCH_MANAGER);
        m.setBranchId(branchId);
        m.setActive(true);
        userRepository.save(m);
    }
}