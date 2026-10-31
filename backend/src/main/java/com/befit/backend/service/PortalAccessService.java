package com.befit.backend.service;

import com.befit.backend.dto.ActivateRequest;
import com.befit.backend.dto.InvitationCheckDTO;
import com.befit.backend.dto.PortalInvitationDTO;
import com.befit.backend.entity.Patient;
import com.befit.backend.entity.PatientInvitation;
import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.PatientInvitationRepository;
import com.befit.backend.repository.PatientRepository;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.security.BranchAccess.PatientAccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Patient portal access for patients registered by STAFF (walk-ins).
 *
 * BUSINESS RULE
 *  - Registering a patient at the front desk NEVER creates a login and never sets a password. The patient starts with
 *    portal access = NONE.
 *  - Staff (ADMIN, or RECEPTIONIST of the patient's own branch) can give the patient an activation link. The link carries a
 *    random 256-bit one-time token, valid for befit.portal.invitation-hours (default 48). Only the token's SHA-256 hash is
 *    stored. Staff hand the link over in person (the patient is at the clinic); creating a new link cancels the old one.
 *  - The patient opens the link, chooses their OWN e-mail and password, and the login is attached to the EXISTING patient
 *    profile (no second profile). Redeeming consumes the token. The patient then signs in as usual - activation does not log
 *    anyone in, so every portal feature still requires authentication.
 *  - Patients who register themselves online get a login at once (UserService.registerPatient) and are refused if a profile
 *    with the same name + phone / date of birth already exists, so a walk-in cannot end up with two profiles.
 */
@Service
public class PortalAccessService {
    private static final Logger log = LoggerFactory.getLogger(PortalAccessService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** One message for every way a link can be unusable (unknown, used, revoked, expired): nothing to learn by guessing. */
    public static final String INVALID_LINK = "This activation link is invalid or has expired. Please ask the clinic for a new one.";

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientInvitationRepository invitations;
    private final PasswordEncoder passwordEncoder;
    private final BranchAccess access;
    private final long invitationHours;

    public PortalAccessService(PatientRepository patientRepository, UserRepository userRepository,
                               PatientInvitationRepository invitations, PasswordEncoder passwordEncoder, BranchAccess access,
                               @Value("${befit.portal.invitation-hours:48}") long invitationHours) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.invitations = invitations;
        this.passwordEncoder = passwordEncoder;
        this.access = access;
        this.invitationHours = Math.max(invitationHours, 1);
    }

    // ------------------------------------------------------------------ staff side

    /** Creates (and returns, once) an activation link for a patient without portal access. Replaces any earlier link. */
    @Transactional
    public PortalInvitationDTO issue(AuthUser staff, Long patientId) {
        Patient p = loadManageable(staff, patientId);
        if (p.getUser() != null) {
            throw ApiException.conflict("ALREADY_HAS_ACCOUNT", "This patient already has portal access.");
        }
        LocalDateTime now = LocalDateTime.now();
        revokeOpen(p.getId(), now);

        String token = newToken();
        PatientInvitation inv = new PatientInvitation(p.getId(), hash(token), staff.email(), now.plusHours(invitationHours));
        inv.setCreatedAt(now);
        invitations.save(inv);
        log.info("Portal invitation created by {} ({}): patient={}, expires={}", staff.email(), staff.role(), p.getId(), inv.getExpiresAt());
        return new PortalInvitationDTO(token, "/activate#t=" + token, inv.getExpiresAt());
    }

    /** Cancels the patient's open activation link(s), e.g. when it was handed to the wrong person. */
    @Transactional
    public void revoke(AuthUser staff, Long patientId) {
        Patient p = loadManageable(staff, patientId);
        int n = revokeOpen(p.getId(), LocalDateTime.now());
        log.info("Portal invitation revoked by {} ({}): patient={}, links={}", staff.email(), staff.role(), p.getId(), n);
    }

    private Patient loadManageable(AuthUser staff, Long patientId) {
        if (staff.role() != Role.ADMIN && staff.role() != Role.RECEPTIONIST) {
            throw ApiException.forbidden("Only receptionists and administrators can give patients portal access.");
        }
        Patient p = patientRepository.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found."));
        if (access.patientAccess(staff, p) != PatientAccessLevel.FULL) {
            throw ApiException.forbidden("Portal access can only be managed by the branch where the patient is registered.");
        }
        return p;
    }

    private int revokeOpen(Long patientId, LocalDateTime now) {
        var open = invitations.findByPatientIdAndUsedAtIsNullAndRevokedAtIsNull(patientId);
        open.forEach(i -> i.setRevokedAt(now));
        invitations.saveAll(open);
        return open.size();
    }

    // ------------------------------------------------------------------ patient side (public endpoints)

    /** Lets the activation page say "Welcome, Asha" or "this link is no longer valid" before asking for a password. */
    @Transactional(readOnly = true)
    public InvitationCheckDTO check(String token) {
        if (token == null || token.isBlank()) return new InvitationCheckDTO(false, null);
        var inv = invitations.findByTokenHash(hash(token.trim()));
        if (inv.isEmpty() || !inv.get().isUsable(LocalDateTime.now())) return new InvitationCheckDTO(false, null);
        Patient p = patientRepository.findById(inv.get().getPatientId()).orElse(null);
        if (p == null || p.getUser() != null) return new InvitationCheckDTO(false, null);
        String name = p.getFullName() == null ? "" : p.getFullName().trim();
        return new InvitationCheckDTO(true, name.isEmpty() ? null : name.split("\\s+")[0]);
    }

    /**
     * Redeems the link: creates the login (role PATIENT) with the patient's own e-mail and password and attaches it to the
     * existing profile. Single use - the invitation row is locked while this runs, so two requests cannot both succeed.
     */
    @Transactional
    public void activate(ActivateRequest r) {
        LocalDateTime now = LocalDateTime.now();
        PatientInvitation inv = invitations.findForUpdateByTokenHash(hash(r.token().trim()))
                .filter(i -> i.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest(INVALID_LINK));
        Patient p = patientRepository.findById(inv.getPatientId()).orElseThrow(() -> ApiException.badRequest(INVALID_LINK));
        if (p.getUser() != null) {                       // got a login another way meanwhile: the link is spent
            inv.setRevokedAt(now);
            invitations.save(inv);
            throw ApiException.badRequest(INVALID_LINK);
        }
        String email = r.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_EXISTS", "An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(p.getFullName());
        user.setEmail(email);
        user.setPhone(p.getPhone());
        user.setPassword(passwordEncoder.encode(r.password()));     // the patient's own password, never generated or shown
        user.setRole(Role.PATIENT);
        user.setActive(true);
        User saved = userRepository.save(user);

        p.setUser(saved);                                            // same profile, now with a login - no duplicate
        patientRepository.save(p);
        inv.setUsedAt(now);
        invitations.save(inv);
        revokeOpen(p.getId(), now);
        log.info("Portal account activated: patient={}", p.getId());
    }

    // ------------------------------------------------------------------ token helpers

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
