package com.befit.backend.service;

import com.befit.backend.dto.AuthResponse;
import com.befit.backend.dto.LoginRequest;
import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.PatientBranchHistory;
import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.BranchRepository;
import com.befit.backend.repository.PatientBranchHistoryRepository;
import com.befit.backend.repository.PatientRepository;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private PatientBranchHistoryRepository historyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    /**
     * Registration creates TWO rows in ONE transaction:
     *   users    -> login only (email, password, role = PATIENT)
     *   patients -> profile + health data, tagged with the chosen branch
     * If either insert fails, neither is saved.
     */
    @Transactional
    public User registerPatient(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw ApiException.conflict("EMAIL_EXISTS", "An account with this email already exists.");
        }
        String branchId = (request.getBranchId() != null && !request.getBranchId().isBlank())
                ? request.getBranchId() : "KARVE-ROAD";
        var branch = branchRepository.findById(branchId)
                .orElseThrow(() -> ApiException.badRequest("Unknown branch: " + branchId));
        if (!branch.isActive()) throw ApiException.badRequest("This branch is not accepting patients right now.");

        // 1) login identity only
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        User savedUser = userRepository.save(user);

        // 2) patient record
        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setFullName(request.getFullName());
        patient.setPhone(request.getPhone());
        patient.setGender(PatientService.normalizeGender(request.getGender()));
        patient.setDateOfBirth(request.getDob());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContact(request.getEmergencyContactNumber());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setBranchId(branchId);
        patient.setRegistrationDate(LocalDate.now());
        if (request.getPainType() != null) {
            patient.setMedicalHistory("Self-Reported Initial Pain: " + request.getPainType()
                    + " (Rating: " + request.getPainRating() + "/10)");
        }
        Patient savedPatient = patientRepository.save(patient);

        // 3) first branch-history row
        PatientBranchHistory first = new PatientBranchHistory(savedPatient.getId(), branchId);
        first.setReason("Registered online");
        historyRepository.save(first);
        return savedUser;
    }

    public AuthResponse loginUser(LoginRequest request) {
        // One message for "no such user", "wrong password" and "disabled account": do not reveal which emails exist.
        User user = userRepository.findByEmail(request.getEmail() == null ? "" : request.getEmail().trim())
                .orElseThrow(() -> new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password."));

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword()) || !user.isActive()) {
            throw new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        // Patients: branch comes from the patient record (the single source of truth, can change).
        // Staff: branch comes from their own user row.
        String branchId = user.getBranchId();
        if (user.getRole() == Role.PATIENT) {
            branchId = patientRepository.findByUserEmail(user.getEmail()).map(Patient::getBranchId).orElse(null);
        }
        return new AuthResponse(token, user.getRole(), user.getFullName(), branchId);
    }
}
