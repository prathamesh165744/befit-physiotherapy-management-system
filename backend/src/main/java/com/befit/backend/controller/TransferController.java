package com.befit.backend.controller;

import com.befit.backend.dto.DecisionBody;
import com.befit.backend.dto.TransferDTO;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Staff view of branch-transfer requests. The RECEIVING branch's receptionist (or an admin) decides. */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;
    private final BranchAccess access;

    public TransferController(TransferService transferService, BranchAccess access) {
        this.transferService = transferService;
        this.access = access;
    }

    /** ?status=PENDING (default) | APPROVED | REJECTED | CANCELLED | ALL */
    @GetMapping
    public List<TransferDTO> list(@RequestParam(required = false) String branchId,
                                  @RequestParam(defaultValue = "PENDING") String status) {
        return transferService.listForStaff(access.current(), branchId, status);
    }

    @PostMapping("/{id}/approve")
    public TransferDTO approve(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionBody body) {
        return transferService.approve(access.current(), id, body == null ? null : body.note());
    }

    @PostMapping("/{id}/reject")
    public TransferDTO reject(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionBody body) {
        return transferService.reject(access.current(), id, body == null ? null : body.note());
    }
}
