package com.befit.backend.config;

import com.befit.backend.entity.Branch;
import com.befit.backend.repository.BranchRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BranchSeeder {
    @Autowired private BranchRepository branchRepository;

    @PostConstruct
    public void seed() {
        save("KARVE-ROAD", "Karve Road, Pune");
        save("BANER", "Baner, Pune");
        save("VIMAN-NAGAR", "Viman Nagar, Pune");
    }
    private void save(String id, String name) {
        if (!branchRepository.existsById(id)) branchRepository.save(new Branch(id, name));
    }
}
