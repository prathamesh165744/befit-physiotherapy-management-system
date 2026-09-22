package com.befit.backend.service;

import com.befit.backend.dto.LoginRequest;
import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public User registerPatient(RegisterRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        
        // --- Map New Patient Demographics & Contact Info ---
        user.setDob(request.getDob());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setEmergencyContactName(request.getEmergencyContactName());
        user.setEmergencyContactNumber(request.getEmergencyContactNumber());
        
        // --- Map New Pain Metrics ---
        user.setPainType(request.getPainType());
        user.setPainRating(request.getPainRating());
        
        return userRepository.save(user);
    }

    public String loginPatient(LoginRequest request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with this email"));
        
        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        // 3. Generate and return JWT
        return jwtUtil.generateToken(user.getEmail());
    }
}