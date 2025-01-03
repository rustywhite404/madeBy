package com.madeby.orderservice.service;

import com.madeby.orderservice.client.CartServiceClient;
import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.dto.OrderRequestDto;
import com.madeby.orderservice.repository.OrderProductSnapshotRepository;
import com.madeby.orderservice.repository.OrderRepository;
import com.madeby.orderservice.repository.PaymentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProductSnapshotRepository snapshotRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private static final Long PRODUCT_INFO_ID = 4L;

    @BeforeEach
    void setUp() {
        // Redis 초기화 (20개 재고 설정)
        redissonClient.getBucket("product_stock:" + PRODUCT_INFO_ID).set(20);

        // 데이터베이스 초기화
        orderRepository.deleteAll();
        snapshotRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void testConcurrentOrders() throws InterruptedException {
        // 주문 요청 정보 설정
        OrderRequestDto requestA = new OrderRequestDto(PRODUCT_INFO_ID, 18); // A 사용자의 주문
        OrderRequestDto requestB = new OrderRequestDto(PRODUCT_INFO_ID, 10); // B 사용자의 주문

        // 스레드 풀 및 CountDownLatch 설정
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        boolean[] results = new boolean[2];

        // A 사용자의 주문 요청
        executorService.submit(() -> {
            try {
                orderService.placeOrder(1L, requestA.getProductInfoId(), requestA.getQuantity());
                results[0] = true;
            } catch (Exception e) {
                results[0] = false;
            } finally {
                latch.countDown();
            }
        });

        // B 사용자의 주문 요청
        executorService.submit(() -> {
            try {
                orderService.placeOrder(2L, requestB.getProductInfoId(), requestB.getQuantity());
                results[1] = true;
            } catch (Exception e) {
                results[1] = false;
            } finally {
                latch.countDown();
            }
        });

        // 모든 작업 대기
        latch.await();
        executorService.shutdown();

        // 테스트 결과 확인
        Assertions.assertTrue(results[0] || results[1], "최소 하나의 주문은 성공해야 합니다.");
        Assertions.assertFalse(results[0] && results[1], "두 주문이 모두 성공하면 안 됩니다.");
    }




}
