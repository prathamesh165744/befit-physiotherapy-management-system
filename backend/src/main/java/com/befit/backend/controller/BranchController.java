package com.befit.backend.controller;

import com.befit.backend.dto.BranchDTO;
import com.befit.backend.dto.BranchRequest;
import com.befit.backend.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {
    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /** Public: active branches (registration page, patient branch picker). */
    @GetMapping
    public List<BranchDTO> active() { return branchService.listActive(); }

    /** ADMIN: every branch including inactive ones. */
    @GetMapping("/all")
    public List<BranchDTO> all() { return branchService.listAll(); }

    /** ADMIN: add a new branch - no code change or restart needed. */
    @PostMapping
    public ResponseEntity<BranchDTO> create(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(201).body(branchService.create(request));
    }

    @PutMapping("/{id}")
    public BranchDTO update(@PathVariable String id, @Valid @RequestBody BranchRequest request) {
        return branchService.update(id, request);
    }
}
