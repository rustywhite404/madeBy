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
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final RedissonClient redissonClient;


    //TODO
    // -비동기 처리나 배치 처리를 통해 성능 개선이 가능할 것 같다.
    // 지금은 타임아웃 된 결제건이 많을 경우 매번 productServiceClient와 연결해서 변경해야 하므로 병목 발생 가능성 있음.
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void checkPaymentTimeouts() {
        // 5분 이상 '결제시도' 또는 '결제중' 상태인 결제 조회
        log.info("----------------------[Scheduler]---------------------");
        log.info("5분 이상 '결제시도' 또는 '결제중' 상태인 결제 실패 처리");
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(5);
        List<Payment> timeoutPayments = paymentRepository.findTimedOutPayments(timeoutTime);

        // 타임아웃 처리, 재고 복구
        for (Payment payment : timeoutPayments) {
            try {
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
                    Integer currentStock = (Integer) redissonClient.getBucket(redisKey).get();
                    if (currentStock != null) {
                        redissonClient.getBucket(redisKey).set(currentStock + quantity);
                    } else {
                        redissonClient.getBucket(redisKey).set(quantity);
                    }

                    log.info("재고 복구 완료 - ProductInfo ID: {}, 복구 수량: {}, 현재 Redis 재고: {}",
                            productInfoId, quantity, redissonClient.getBucket(redisKey).get());
                }

                log.info("결제 실패 처리 완료 - Payment ID: {}, Order ID: {}", payment.getId(), payment.getOrderId());
            } catch (Exception e) {
                log.error("결제 실패 처리 중 오류 발생 - Payment ID: {}, Error: {}", payment.getId(), e.getMessage());
            }
        }
    }
}