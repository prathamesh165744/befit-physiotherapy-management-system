package com.befit.backend;

import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CrudTestRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    public CrudTestRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- STARTING CRUD TEST ---");

        // 1. Create (Insert)
        User newUser = new User();
        newUser.setFullName("Test Patient");
        newUser.setEmail("test.patient@befit.com");
        newUser.setPhone("1234567890");
        newUser.setPassword("securepass123"); 
        newUser.setRole(Role.PATIENT);
        userRepository.save(newUser);
        System.out.println("User created with ID: " + newUser.getId());

        // 2. Read (Select)
        List<User> users = userRepository.findAll();
        System.out.println("Total users in database: " + users.size());
        users.forEach(u -> System.out.println("Found User: " + u.getFullName() + " | Email: " + u.getEmail()));

        // 3. Update
        User existingUser = userRepository.findById(newUser.getId()).orElse(null);
        if (existingUser != null) {
            existingUser.setPhone("0987654321");
            userRepository.save(existingUser);
            System.out.println("User phone updated to: " + existingUser.getPhone());
        }

        // 4. Delete (Commented out so you can verify the data in Neon DB)
        // userRepository.deleteById(newUser.getId());
        // System.out.println("User deleted.");

        System.out.println("--- CRUD TEST COMPLETE ---");
    }
}