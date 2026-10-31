package com.befit.backend.controller;

import com.befit.backend.dto.ActivateRequest;
import com.befit.backend.dto.AuthResponse;
import com.befit.backend.dto.InvitationCheckDTO;
import com.befit.backend.dto.LoginRequest;
import com.befit.backend.dto.RegisterRequest;
import com.befit.backend.entity.User;
import com.befit.backend.service.PortalAccessService;
import com.befit.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PortalAccessService portalAccessService;

    public AuthController(UserService userService, PortalAccessService portalAccessService) {
        this.userService = userService;
        this.portalAccessService = portalAccessService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = userService.registerPatient(request);
        return ResponseEntity.ok(Map.of("message", "Patient registered successfully!", "userId", registeredUser.getId()));
    }

    /** Success body is unchanged: { token, role, fullName, branchId }. Failures are 401 { code, message }. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.loginUser(request));
    }

    /** Is this activation link still usable? Answers only { valid, firstName }. POST so the token never sits in a URL. */
    @PostMapping("/invitations/check")
    public InvitationCheckDTO checkInvitation(@RequestBody InvitationCheckDTO.Request request) {
        return portalAccessService.check(request == null ? null : request.token());
    }

    /** Redeems an activation link: the patient chooses their own e-mail + password. Does NOT sign anyone in. */
    @PostMapping("/activate")
    public ResponseEntity<?> activate(@Valid @RequestBody ActivateRequest request) {
        portalAccessService.activate(request);
        return ResponseEntity.ok(Map.of("message", "Your account is ready. Please sign in."));
    }
}
