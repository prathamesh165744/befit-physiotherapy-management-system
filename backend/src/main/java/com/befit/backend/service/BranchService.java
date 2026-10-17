package com.befit.backend.service;

import com.befit.backend.dto.BranchDTO;
import com.befit.backend.dto.BranchRequest;
import com.befit.backend.entity.Branch;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BranchService {
    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /** Public list for the registration page / patient branch switcher: active branches only. */
    @Transactional(readOnly = true)
    public List<BranchDTO> listActive() {
        return branchRepository.findByActiveTrueOrderByNameAsc().stream().map(BranchDTO::of).toList();
    }

    @Transactional(readOnly = true)
    public List<BranchDTO> listAll() {
        return branchRepository.findAllByOrderByNameAsc().stream().map(BranchDTO::of).toList();
    }

    /** id -> display name, one small query. Used to label rows without a query per row. */
    @Transactional(readOnly = true)
    public Map<String, String> nameMap() {
        Map<String, String> m = new HashMap<>();
        for (Branch b : branchRepository.findAll()) m.put(b.getId(), b.getName());
        return m;
    }

    @Transactional(readOnly = true)
    public Branch require(String id) {
        return branchRepository.findById(id).orElseThrow(() -> ApiException.notFound("Branch not found."));
    }

    @Transactional
    public BranchDTO create(BranchRequest r) {
        if (branchRepository.existsById(r.id())) throw ApiException.conflict("BRANCH_EXISTS", "A branch with this id already exists.");
        Branch b = new Branch(r.id(), r.name().trim(), r.address(), r.phone(), r.email());
        b.setActive(r.active() == null || r.active());
        return BranchDTO.of(branchRepository.save(b));
    }

    @Transactional
    public BranchDTO update(String id, BranchRequest r) {
        Branch b = require(id);
        b.setName(r.name().trim());
        b.setAddress(r.address());
        b.setPhone(r.phone());
        b.setEmail(r.email());
        if (r.active() != null) b.setActive(r.active());
        return BranchDTO.of(branchRepository.save(b));
    }
}
