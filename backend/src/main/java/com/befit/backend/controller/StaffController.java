package com.befit.backend.controller;

import com.befit.backend.dto.DoctorDTO;
import com.befit.backend.dto.ManagerRequest;
import com.befit.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final UserService userService;

    public StaffController(UserRepository userRepository, BranchAccess access, UserService userService) {
        this.userRepository = userRepository;
        this.access = access;
        this.userService = userService;
    }

    /** Admin only (SecurityConfig + check here): create a BRANCH_MANAGER tied to one branch. */
    @PostMapping("/managers")
    public DoctorDTO createManager(@Valid @RequestBody ManagerRequest r) {
        if (!access.current().isAdmin()) throw com.befit.backend.exception.ApiException.forbidden("Only an administrator can create branch managers.");
        var u = userService.createBranchManager(r.fullName(), r.email(), r.phone(), r.password(), r.branchId());
        return new DoctorDTO(u.getId(), u.getFullName(), u.getBranchId());
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
