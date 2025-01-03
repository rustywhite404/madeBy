package com.madeby.orderservice.service;

import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.entity.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@SpringBootTest
public class HighConcurrencyOrderTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private RedissonClient redissonClient;

    private static final Long PRODUCT_INFO_ID = 8L;
    private static final int INITIAL_STOCK = 20;

    @BeforeEach
    void setUp() {
        // Redis 초기화 (20개 재고 설정)
        redissonClient.getBucket("product_stock:" + PRODUCT_INFO_ID).set(INITIAL_STOCK);

        // ProductServiceClient를 통해 DB에도 재고 초기화
        productServiceClient.updateStock(PRODUCT_INFO_ID, INITIAL_STOCK);
    }

    @Test
    void testHighConcurrencyOrders() throws InterruptedException {
        int numberOfUsers = 100; // 동시 주문 사용자 수
        int threadPoolSize = 50; // 스레드풀 크기 설정
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);

        // 각 사용자 결과 저장
        PaymentStatus[] results = new PaymentStatus[numberOfUsers];

        // 각 사용자 주문 실행
        for (int i = 0; i < numberOfUsers; i++) {
            int userId = i + 1; // 사용자 ID
            executorService.submit(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 100)); // 0~100ms 간격
                    results[userId - 1] = orderService.placeOrder((long) userId, PRODUCT_INFO_ID, 1); // 결제 상태 저장
                } catch (Exception e) {
                    results[userId - 1] = PaymentStatus.FAILED; // 실패로 처리
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 작업 대기
        latch.await();
        executorService.shutdown();

        // 테스트 결과 분석
        long successfulOrders = countResults(results, PaymentStatus.COMPLETED);
        long failedOrders = countResults(results, PaymentStatus.FAILED);
        long canceledOrders = countResults(results, PaymentStatus.CANCELED);

        System.out.println("성공한 주문 수: " + successfulOrders);
        System.out.println("실패한 주문 수(재고 부족): " + failedOrders);
        System.out.println("취소된 주문 수(결제 이탈): " + canceledOrders);

        // 검증 로직
        Assertions.assertEquals(INITIAL_STOCK, successfulOrders, "재고를 초과하지 않도록 성공한 주문은 재고와 일치해야 합니다.");
        Assertions.assertEquals(numberOfUsers - successfulOrders - canceledOrders, failedOrders, "실패한 주문 수는 재고 초과로 인해 발생한 주문 수와 일치해야 합니다.");
    }
    @AfterEach
    void tearDown() {
        // Redis와 DB 정리
        redissonClient.getBucket("product_stock:" + PRODUCT_INFO_ID).delete();
    }

    private long countResults(PaymentStatus[] results, PaymentStatus expectedStatus) {
        return Arrays.stream(results)
                .filter(result -> result == expectedStatus)
                .count();
    }

}
