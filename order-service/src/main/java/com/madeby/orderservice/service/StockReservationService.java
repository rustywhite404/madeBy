package com.madeby.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
        return decrementStockWithLua(productInfoId, quantity);
    }

    private boolean decrementStockWithLua(Long productInfoId, int quantity) {
        // Lua 스크립트 실행 로직
        String stockKey = "product_stock:" + productInfoId;
        String luaScript = """
                    local stockKey = KEYS[1]
                    local quantity = tonumber(ARGV[1])
                    local currentStock = tonumber(redis.call('GET', stockKey))
                    if currentStock == nil or currentStock < quantity then
                        return -1
                    end
                    local newStock = redis.call('DECRBY', stockKey, quantity) -- 재고 감소 후 남은 재고
                                        return newStock -- 남은 재고 반환
                """;

        Object result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(stockKey),
                quantity
        );

        if ((Long) result == -1) {
            log.info("재고 부족: productInfoId = {}, 요청 수량 = {}", productInfoId, quantity);
            return false;
        }

        log.info("[재고 감소 완료] 남은 재고 : {}", result);
        return true;
    }

    /**
     * 재고 예약 취소 (Redis 재고 복구)
     */
    public void cancelReservation(Long productInfoId, int quantity) {
        String redisKey = STOCK_KEY + productInfoId;

        String luaScript = """
            local stockKey = KEYS[1]
            local quantity = tonumber(ARGV[1])
            redis.call('INCRBY', stockKey, quantity)
            return 1
        """;

        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey),
                quantity
        );
        log.info("재고 복구 완료: productInfoId = {}, quantity = {}", productInfoId, quantity);
    }
}
