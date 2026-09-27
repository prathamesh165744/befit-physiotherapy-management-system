package com.befit.backend.service;

import com.befit.backend.dto.LoginRequest;
import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.repository.PatientRepository;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository; 
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    public User registerPatient(RegisterRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        user.setDob(request.getDob() != null ? request.getDob().toString() : null);
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setEmergencyContactName(request.getEmergencyContactName());
        user.setEmergencyContactNumber(request.getEmergencyContactNumber());
        user.setPainType(request.getPainType());
        user.setPainRating(request.getPainRating());
        
        // Link the user login account to their branch!
        String assignedBranch = request.getBranchId() != null && !request.getBranchId().isEmpty() ? request.getBranchId() : "KARVE-ROAD";
        user.setBranchId(assignedBranch);
        
        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setFullName(request.getFullName());
        patient.setPhone(request.getPhone());
        patient.setGender(request.getGender());
        patient.setDateOfBirth(request.getDob());
        patient.setEmergencyContact(request.getEmergencyContactNumber());
        patient.setBranchId(assignedBranch);
        patient.setRegistrationDate(LocalDate.now());
        
        if (request.getPainType() != null) {
            patient.setMedicalHistory("Self-Reported Initial Pain: " + request.getPainType() + " (Rating: " + request.getPainRating() + "/10)");
        }
        patientRepository.save(patient);
        return savedUser;
    }

    public com.befit.backend.dto.AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with this email"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        String token = jwtUtil.generateToken(user.getEmail());
        // Pass the branchId to the frontend!
        return new com.befit.backend.dto.AuthResponse(token, user.getRole(), user.getFullName(), user.getBranchId());
    }
}