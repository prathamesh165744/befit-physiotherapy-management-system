package com.befit.backend.repository;

import com.befit.backend.entity.Role;
import com.befit.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleAndBranchIdInAndActiveTrueOrderByFullNameAsc(Role role, Collection<String> branchIds);
    List<User> findByBranchIdIsNullAndRoleIn(Collection<Role> roles);
}
