package com.befit.backend.repository;

import com.befit.backend.entity.BillAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillAuditLogRepository extends JpaRepository<BillAuditLog, Long> {
    List<BillAuditLog> findByBillIdOrderByChangedAtDescIdDesc(Long billId);
}
