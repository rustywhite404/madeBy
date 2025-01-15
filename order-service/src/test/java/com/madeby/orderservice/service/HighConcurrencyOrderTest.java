package com.madeby.orderservice.service;

import com.madeby.orderservice.client.ProductServiceClient;
import com.madeBy.shared.entity.PaymentStatus;
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

@SpringBootTest
public class HighConcurrencyOrderTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private RedissonClient redissonClient;

    private static final Long PRODUCT_INFO_ID = 32L;
    private static final int INITIAL_STOCK = 10;

    @BeforeEach
    void setUp() {
        // Redis 초기화 (20개 재고 설정)
        redissonClient.getBucket("product_stock:" + PRODUCT_INFO_ID).set(INITIAL_STOCK);

        // ProductServiceClient를 통해 DB에도 재고 초기화
        productServiceClient.updateStock(PRODUCT_INFO_ID, INITIAL_STOCK);
    }

    @Test
    void testHighConcurrencyOrders() throws InterruptedException {
        int numberOfUsers = 10; // 동시 주문 사용자 수
        int threadPoolSize = 100; // 스레드풀 크기 설정
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(numberOfUsers);

        // 각 사용자 결과 저장
        PaymentStatus[] results = new PaymentStatus[numberOfUsers];

        // 수행 시간 측정을 위한 시작 시간 기록
        long startTime = System.currentTimeMillis();

        // 각 사용자 주문 실행
        for (int i = 0; i < numberOfUsers; i++) {
            int userId = i + 1; // 사용자 ID
            executorService.submit(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 100)); // 0~100ms 간격
                    //results[userId - 1] = orderService.placeOrder((long) userId, PRODUCT_INFO_ID, 1); // 결제 상태 저장
                } catch (Exception e) {
                    results[userId - 1] = PaymentStatus.SOLD_OUT; // 재고 부족으로 실패
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 작업 대기
        latch.await();
        executorService.shutdown();

        // 수행 시간 측정을 위한 종료 시간 기록
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 테스트 결과 분석
        long successfulOrders = countResults(results, PaymentStatus.COMPLETED);
        long failedOrders = countResults(results, PaymentStatus.FAILED);
        long canceledOrders = countResults(results, PaymentStatus.CANCELED);
        long soldOutOrders = countResults(results, PaymentStatus.SOLD_OUT);

        System.out.println("성공한 주문 수: " + successfulOrders);
        System.out.println("실패한 주문 수(결제 실패): " + failedOrders);
        System.out.println("실패한 주문 수(재고 부족): " + soldOutOrders);
        System.out.println("취소된 주문 수(결제 이탈): " + canceledOrders);

        // 총 수행 시간 출력
        System.out.println("총 수행 시간(ms): " + totalTime);

        // 검증 로직
        Assertions.assertEquals(INITIAL_STOCK, successfulOrders, "재고를 초과하지 않도록 성공한 주문은 재고와 일치해야 합니다.");
        Assertions.assertEquals(numberOfUsers - successfulOrders - canceledOrders - soldOutOrders, failedOrders, "실패한 주문 수는 결제 실패, 재고 부족, 결제 이탈을 합친 수와 일치해야 합니다.");
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
