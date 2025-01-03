package com.madeby.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockReservationService {
    private final RedissonClient redissonClient;
    private static final String STOCK_KEY = "product_stock:";

    /**
     * 재고 확인 및 예약
     */
    public boolean reserveStock(Long productInfoId, int quantity) {
        String lockKey = "lock:product_stock:" + productInfoId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도
            if (!lock.tryLock(60, 30, TimeUnit.SECONDS)) {
                return false; // 다른 스레드가 처리 중
            }

            // Redis에서 재고 확인 및 감소
            String redisKey = STOCK_KEY + productInfoId;
            Integer currentStock = (Integer) redissonClient.getBucket(redisKey).get();

            if (currentStock == null || currentStock < quantity) {
                return false; // 재고 부족
            }

            // 재고 감소
            redissonClient.getBucket(redisKey).set(currentStock - quantity);
            log.info("재고 감소: productInfoId = {}, 감소량 = {}, 남은 재고 = {}", productInfoId, quantity, currentStock - quantity);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 재고 예약 취소
     */
    public void cancelReservation(Long productInfoId, int quantity) {
        String redisKey = STOCK_KEY + productInfoId;
        RLock lock = redissonClient.getLock("lock:product_stock:" + productInfoId);

        try {
            lock.lock();
            Integer currentStock = (Integer) redissonClient.getBucket(redisKey).get();
            if (currentStock != null) {
                redissonClient.getBucket(redisKey).set(currentStock + quantity);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
