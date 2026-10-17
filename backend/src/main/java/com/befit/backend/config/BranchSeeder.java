package com.befit.backend.config;

import com.befit.backend.entity.Branch;
import com.befit.backend.repository.BranchRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 1 of 4 on startup: makes sure the branches exist (with address / phone).
 * Safe to run on every start: existing branches are kept, only EMPTY detail fields are filled in.
 * Adding a branch later does not need code: an ADMIN can POST /api/branches.
 */
@Component
@Order(1)
public class BranchSeeder implements CommandLineRunner {
    private final BranchRepository branchRepository;

    public BranchSeeder(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensure("KARVE-ROAD", "Karve Road, Pune", "12th Floor, BeFit Tower, Karve Road, Pune 411004", "+91 20 4000 1001", "karveroad@befit.com", true);
        ensure("BANER", "Baner, Pune", "2nd Floor, Orbit Plaza, Baner Road, Pune 411045", "+91 20 4000 1002", "baner@befit.com", true);
        ensure("VIMAN-NAGAR", "Viman Nagar, Pune", "Ground Floor, Sky Court, Viman Nagar, Pune 411014", "+91 20 4000 1003", "vimannagar@befit.com", true);
        ensure("KOTHRUD", "Kothrud, Pune", "1st Floor, Paud Road Arcade, Kothrud, Pune 411038", "+91 20 4000 1004", "kothrud@befit.com", true);
        // An inactive branch, to show that inactive branches cannot be chosen for registration / transfer / booking.
        ensure("WAKAD", "Wakad, Pune (opening soon)", "Wakad Road, Pune 411057", "+91 20 4000 1005", "wakad@befit.com", false);
    }

    private void ensure(String id, String name, String address, String phone, String email, boolean active) {
        Branch b = branchRepository.findById(id).orElse(null);
        if (b == null) {
            b = new Branch(id, name, address, phone, email);
            b.setActive(active);
            branchRepository.save(b);
            return;
        }
        boolean changed = false;
        if (b.getAddress() == null) { b.setAddress(address); changed = true; }
        if (b.getPhone() == null) { b.setPhone(phone); changed = true; }
        if (b.getEmail() == null) { b.setEmail(email); changed = true; }
        if (changed) branchRepository.save(b);
    }
}
