package com.madeby.payservice.service;

import com.madeBy.shared.entity.PaymentStatus;
import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.payservice.entity.Payment;
import com.madeby.payservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayService {
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentStatus processPayment(Long orderId, Long userId) {
        // 1. 결제 데이터 조회
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PAYMENT));

        //2. 고객 이탈율 시뮬레이션 (20% 확률로 결제 시도 중단)
        if (Math.random() < 0.2) {
            payment.setStatus(PaymentStatus.CANCELED);
            return PaymentStatus.CANCELED;
        }

        // 3. 결제 상태 변경 ('결제중')
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // 4. 결제 완료 시뮬레이션 (20% 확률로 결제 실패)
        if (Math.random() < 0.2) {
            payment.setStatus(PaymentStatus.FAILED);
            return PaymentStatus.FAILED;
        }

        // 5. 결제 성공
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return PaymentStatus.COMPLETED;
    }


    @Transactional
    public void initiatePayment(Long orderId, Long userId) {
        // 1. 결제 데이터 조회
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> Payment.builder()
                        .orderId(orderId)
                        .status(PaymentStatus.PENDING) // '결제시도' 상태
                        .build());

        // 2. 결제 데이터 새로 생성 or 업데이트
        paymentRepository.save(payment);
        log.info("[PayService] initiatePayment - 결제 정보 초기화 완료. orderId: {}, userId: {}, status: {}",
                orderId, userId, payment.getStatus());
    }
}
