package com.madeby.orderservice.scheduler;

import com.madeby.orderservice.entity.Payment;
import com.madeby.orderservice.entity.PaymentStatus;
import com.madeby.orderservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentRepository paymentRepository;

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void checkPaymentTimeouts() {
        // 5분 이상 '결제시도' 또는 '결제중' 상태인 결제 조회
        log.info("----------------------[Scheduler]---------------------");
        log.info("5분 이상 '결제시도' 또는 '결제중' 상태인 결제 실패 처리");
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(5);
        List<Payment> timeoutPayments = paymentRepository.findTimedOutPayments(timeoutTime);

        // 타임아웃 처리
        for (Payment payment : timeoutPayments) {
            payment.setStatus(PaymentStatus.CANCELED);
            paymentRepository.save(payment);
        }
    }
}