package com.befit.backend.controller;

import com.befit.backend.dto.*;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.PatientService;
import com.befit.backend.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final TransferService transferService;
    private final BranchAccess access;

    public PatientController(PatientService patientService, TransferService transferService, BranchAccess access) {
        this.patientService = patientService;
        this.transferService = transferService;
        this.access = access;
    }

    // ---------------------------------------------------------------- staff (ADMIN / DOCTOR / RECEPTIONIST)

    /** Paged directory. scope=current (default) | all (also patients who moved away but have visits/invoices here). */
    @GetMapping
    public PageResponse<PatientSummaryDTO> list(@RequestParam(required = false) String branchId,
                                                @RequestParam(defaultValue = "current") String scope,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "id") String sort,
                                                @RequestParam(defaultValue = "desc") String dir) {
        return patientService.list(access.current(), branchId, scope, q, page, size, sort, dir);
    }

    /** Dropdown list (id, name, phone) of current patients - used by the New Appointment form. */
    @GetMapping("/options")
    public List<PatientSummaryDTO> options(@RequestParam(required = false) String branchId) {
        return patientService.options(access.current(), branchId);
    }

    /** "View File" */
    @GetMapping("/{id}")
    public PatientProfileDTO getById(@PathVariable Long id) {
        return patientService.getForStaff(access.current(), id);
    }

    @GetMapping("/{id}/history")
    public PatientHistoryDTO history(@PathVariable Long id) {
        return patientService.historyForStaff(access.current(), id);
    }

    @PostMapping
    public PatientSummaryDTO register(@Valid @RequestBody PatientRegistrationRequest request) {
        return patientService.register(access.current(), request);
    }

    /** ADMIN only: move a patient to another branch immediately. */
    @PostMapping("/{id}/transfer")
    public TransferDTO adminTransfer(@PathVariable Long id, @Valid @RequestBody TransferRequestBody body) {
        return transferService.adminTransfer(access.current(), id, body);
    }

    // ---------------------------------------------------------------- the logged-in patient (role PATIENT, identified by the token)

    @GetMapping("/me")
    public PatientProfileDTO myProfile() {
        return patientService.myProfile(access.current());
    }

    @PutMapping("/me")
    public PatientProfileDTO updateMyProfile(@Valid @RequestBody PatientProfileUpdate update) {
        return patientService.updateMyProfile(access.current(), update);
    }

    @GetMapping("/me/history")
    public PatientHistoryDTO myHistory() {
        return patientService.myHistory(access.current());
    }

    /**
     * Body: { "branchId": "BANER", "reason": "optional" }.
     * With approval required (default) this creates a PENDING request; otherwise the move happens immediately.
     */
    @PutMapping("/me/branch")
    public TransferDTO requestBranchChange(@Valid @RequestBody TransferRequestBody body) {
        return transferService.requestTransfer(access.current(), body);
    }

    @GetMapping("/me/transfers")
    public List<TransferDTO> myTransfers() {
        return transferService.myTransfers(access.current());
    }

    @DeleteMapping("/me/branch-request")
    public TransferDTO cancelMyRequest() {
        return transferService.cancelMine(access.current());
    }
}
