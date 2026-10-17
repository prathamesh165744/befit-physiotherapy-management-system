package com.befit.backend.repository;

import com.befit.backend.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findByActiveTrueOrderByNameAsc();
    List<Branch> findAllByOrderByNameAsc();

    @Query("select b.id from Branch b")
    List<String> findAllIds();
}
