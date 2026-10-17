package com.befit.backend.repository;

import com.befit.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillIdOrderByPaymentDateAscIdAsc(Long billId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
