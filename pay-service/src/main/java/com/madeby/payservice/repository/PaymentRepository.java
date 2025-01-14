package com.madeby.payservice.repository;


import com.madeby.payservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 5분 이상 '결제시도' 또는 '결제중' 상태인 결제를 조회
    @Query("SELECT p FROM Payment p " +
            "WHERE (p.status = 'PENDING' OR p.status = 'PROCESSING') " +
            "AND p.modifiedAt < :timeoutTime")
    List<Payment> findTimedOutPayments(LocalDateTime timeoutTime);

    Optional<Payment> findByOrderId(Long orderId);
}
