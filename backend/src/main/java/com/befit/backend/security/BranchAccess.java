package com.befit.backend.security;

import com.befit.backend.entity.Branch;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.BranchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * THE place where branch rules are enforced on the server. Controllers/services never trust a branchId
 * coming from the browser: they ask this class what the logged-in user is allowed to see or create.
 *
 *  - RECEPTIONIST / DOCTOR : exactly their own branch (the ?branchId= the frontend sends is ignored)
 *  - ADMIN                 : any branch, or all of them (?branchId=ALL or no value)
 */
@Component
public class BranchAccess {
    public static final String ALL = "ALL";
    private final BranchRepository branchRepository;

    public BranchAccess(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public AuthUser current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthUser u)) {
            throw new ApiException(401, "UNAUTHENTICATED", "Please log in to continue.");
        }
        return u;
    }

    /** Branches whose records this user may READ. */
    public List<String> visibleBranchIds(AuthUser user, String requested) {
        if (user.isAdmin()) {
            if (requested == null || requested.isBlank() || ALL.equalsIgnoreCase(requested)) {
                return branchRepository.findAllIds();
            }
            if (!branchRepository.existsById(requested)) throw ApiException.badRequest("Unknown branch: " + requested);
            return List.of(requested);
        }
        return List.of(requireOwnBranch(user));
    }

    /** The single branch a NEW record (appointment, invoice, patient) will be created in. */
    public String branchForCreate(AuthUser user, String requested) {
        if (!user.isAdmin()) return requireOwnBranch(user);
        if (requested == null || requested.isBlank() || ALL.equalsIgnoreCase(requested)) {
            throw ApiException.badRequest("Please choose a branch.");
        }
        Branch b = branchRepository.findById(requested).orElseThrow(() -> ApiException.badRequest("Unknown branch: " + requested));
        if (!b.isActive()) throw ApiException.badRequest("That branch is not active.");
        return b.getId();
    }

    /** Throws 403 unless the user may touch a record that belongs to recordBranchId. */
    public void requireBranch(AuthUser user, String recordBranchId) {
        if (user.isAdmin()) return;
        if (recordBranchId == null || !recordBranchId.equals(user.branchId())) {
            throw ApiException.forbidden("You do not have access to records of another branch.");
        }
    }

    private String requireOwnBranch(AuthUser user) {
        if (user.branchId() == null || user.branchId().isBlank()) {
            throw ApiException.forbidden("Your account is not assigned to a branch. Please contact an administrator.");
        }
        return user.branchId();
    }
}
