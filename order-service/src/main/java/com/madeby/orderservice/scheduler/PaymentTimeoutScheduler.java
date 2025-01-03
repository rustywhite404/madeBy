package com.madeby.orderservice.scheduler;

import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.entity.OrderProductSnapshot;
import com.madeby.orderservice.entity.Orders;
import com.madeby.orderservice.entity.Payment;
import com.madeby.orderservice.entity.PaymentStatus;
import com.madeby.orderservice.repository.OrderRepository;
import com.madeby.orderservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final RedissonClient redissonClient;

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void checkPaymentTimeouts() {
        log.info("----------------------[Scheduler]---------------------");
        log.info("5분 이상 '결제시도' 또는 '결제중' 상태인 결제 실패 처리");
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(5);
        List<Payment> timeoutPayments = paymentRepository.findTimedOutPayments(timeoutTime);

        for (Payment payment : timeoutPayments) {
            try {
                processTimeoutPayment(payment);
            } catch (Exception e) {
                log.error("결제 실패 처리 중 오류 발생 - Payment ID: {}, Error: {}", payment.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processTimeoutPayment(Payment payment) {
        // 1. 결제 상태를 CANCELED로 변경
        payment.setStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);

        // 2. 관련 주문 조회
        Orders order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("해당 결제에 대한 주문이 존재하지 않습니다. 결제 ID: " + payment.getId()));

        // 3. 주문 상품 스냅샷을 기반으로 재고 복구
        for (OrderProductSnapshot snapshot : order.getOrderProductSnapshots()) {
            Long productInfoId = snapshot.getProductInfoId();
            int quantity = snapshot.getQuantity();

            // DB 재고 복구
            productServiceClient.updateStock(productInfoId, quantity);

            // Redis 재고 복구
            String redisKey = "product_stock:" + productInfoId;
            redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    "redis.call('INCRBY', KEYS[1], ARGV[1])",
                    RScript.ReturnType.STATUS,
                    Collections.singletonList(redisKey),
                    quantity
            );

            log.info("재고 복구 완료 - ProductInfo ID: {}, 복구 수량: {}", productInfoId, quantity);
        }

        log.info("결제 실패 처리 완료 - Payment ID: {}, Order ID: {}", payment.getId(), payment.getOrderId());
    }
}
