package com.befit.backend.controller;

import com.befit.backend.dto.DecisionBody;
import com.befit.backend.dto.StaffTransferRequest;
import com.befit.backend.dto.TransferDTO;
import com.befit.backend.dto.TransferPermissionsDTO;
import com.befit.backend.dto.TransferValidationDTO;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Staff side of branch transfers (ADMIN / BRANCH_MANAGER / RECEPTIONIST - see SecurityConfig, and re-checked in TransferService).
 * The RECEIVING branch's manager (or an admin) decides; receptionists raise and withdraw but cannot approve or reject while
 * befit.transfer.manager-approval-required=true. Managers are limited to their own branch.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;
    private final BranchAccess access;

    public TransferController(TransferService transferService, BranchAccess access) {
        this.transferService = transferService;
        this.access = access;
    }

    /** What the caller may do - drives which buttons the screens show. */
    @GetMapping("/permissions")
    public TransferPermissionsDTO permissions() {
        return transferService.permissionsFor(access.current());
    }

    /** ?status=PENDING (default) | APPROVED | REJECTED | CANCELLED | ALL   &direction=INCOMING (default) | OUTGOING */
    @GetMapping
    public List<TransferDTO> list(@RequestParam(required = false) String branchId,
                                  @RequestParam(defaultValue = "PENDING") String status,
                                  @RequestParam(defaultValue = "INCOMING") String direction) {
        return transferService.listForStaff(access.current(), branchId, status, direction);
    }

    /** Raise a transfer request for a patient. Always PENDING until the receiving branch (or an admin) approves. */
    @PostMapping
    public TransferDTO create(@Valid @RequestBody StaffTransferRequest body) {
        return transferService.createByStaff(access.current(), body);
    }

    /** Open appointments, upcoming visits and unpaid invoices that the approver must consider. */
    @GetMapping("/{id}/validation")
    public TransferValidationDTO validation(@PathVariable Long id) {
        return transferService.validationFor(access.current(), id);
    }

    /** Body: { "note": "...", "acknowledgeWarnings": true } - the acknowledgement + note are required when the validation has warnings. */
    @PostMapping("/{id}/approve")
    public TransferDTO approve(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionBody body) {
        return transferService.approve(access.current(), id, body == null ? null : body.note(),
                body != null && Boolean.TRUE.equals(body.acknowledgeWarnings()));
    }

    @PostMapping("/{id}/reject")
    public TransferDTO reject(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionBody body) {
        return transferService.reject(access.current(), id, body == null ? null : body.note());
    }

    @PostMapping("/{id}/cancel")
    public TransferDTO cancel(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionBody body) {
        return transferService.cancel(access.current(), id, body == null ? null : body.note());
    }
}
