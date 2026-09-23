package com.befit.backend.config;

import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
            doctor.setActive(true);
            userRepository.save(doctor);

            // 3. Create Receptionist
            User receptionist = new User();
            receptionist.setFullName("Front Desk");
            receptionist.setEmail("reception@befit.com");
            receptionist.setPhone("3333333333");
            receptionist.setPassword(passwordEncoder.encode("staff123"));
            receptionist.setRole(Role.RECEPTIONIST);
            receptionist.setActive(true);
            userRepository.save(receptionist);

            System.out.println("✅ Test Staff Accounts Seeded Successfully!");
        } else {
            System.out.println("✅ Staff Accounts Already Exist. Skipping Seeder.");
        }
    }
}