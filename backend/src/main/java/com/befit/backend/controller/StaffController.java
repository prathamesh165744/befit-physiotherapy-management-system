package com.befit.backend.controller;

import com.befit.backend.dto.DoctorDTO;
import com.befit.backend.entity.Role;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.security.AuthUser;
import com.befit.backend.security.BranchAccess;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final UserRepository userRepository;
    private final BranchAccess access;

    public StaffController(UserRepository userRepository, BranchAccess access) {
        this.userRepository = userRepository;
        this.access = access;
    }

    /** Active doctors of the caller's branch (admins: ?branchId=...). Feeds the "New Appointment" form. */
    @GetMapping("/doctors")
    public List<DoctorDTO> doctors(@RequestParam(required = false) String branchId) {
        AuthUser user = access.current();
        List<String> ids = access.visibleBranchIds(user, branchId);
        return userRepository.findByRoleAndBranchIdInAndActiveTrueOrderByFullNameAsc(Role.DOCTOR, ids).stream()
                .map(u -> new DoctorDTO(u.getId(), u.getFullName(), u.getBranchId())).toList();
    }
}
