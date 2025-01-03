package com.madeby.orderservice.service;

import com.madeby.orderservice.client.ProductServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@SpringBootTest
public class HighConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private RedissonClient redissonClient;

    private static final Long PRODUCT_INFO_ID = 6L;
    private static final int INITIAL_STOCK = 30;

    @BeforeEach
    void setUp() {
        // Redis 초기화 (100개 재고 설정)
        redissonClient.getBucket("product_stock:" + PRODUCT_INFO_ID).set(INITIAL_STOCK);

        // ProductServiceClient를 통해 DB에도 재고 초기화
        productServiceClient.updateStock(PRODUCT_INFO_ID, INITIAL_STOCK);
    }

    @Test
    void testHighConcurrencyOrders() throws InterruptedException {
        int numberOfUsers = 40; // 동시 주문 사용자 수
        int threadPoolSize = 50; // 스레드풀 크기 설정
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize); // 스레드풀 크기 조정
        CountDownLatch latch = new CountDownLatch(numberOfUsers);

        // 각 사용자 결과 저장
        boolean[] results = new boolean[numberOfUsers];

        // 각 사용자 주문 실행
        for (int i = 0; i < numberOfUsers; i++) {
            int userId = i + 1; // 사용자 ID
            executorService.submit(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 100)); // 0~100ms 간격
                    orderService.placeOrder((long) userId, PRODUCT_INFO_ID, 1); // 각 사용자 1개씩 주문
                    results[userId - 1] = true;
                } catch (Exception e) {
                    results[userId - 1] = false;
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 작업 대기
        latch.await();
        executorService.shutdown();

        // 테스트 결과 확인
        long successfulOrders = countSuccessfulOrders(results);
        long failedOrders = numberOfUsers - successfulOrders;

        System.out.println("성공한 주문 수: " + successfulOrders);
        System.out.println("실패한 주문 수: " + failedOrders);

        Assertions.assertEquals(INITIAL_STOCK, successfulOrders, "재고를 초과하지 않도록 성공한 주문은 재고와 일치해야 합니다.");
        Assertions.assertEquals(40, failedOrders, "실패한 주문은 40개여야 합니다.");
    }

    @AfterEach
    void tearDown() {
        // Redis와 DB 정리
        redissonClient.getBucket("product_stock:" + PRODUCT_INFO_ID).delete();
    }

    private long countSuccessfulOrders(boolean[] results) {
        return IntStream.range(0, results.length)
                .filter(i -> results[i])
                .count();
    }
}
