package com.madeby.payservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceClient {

    private final ProductServiceFeignClient feignClient;

    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "updateStockFallback")
    @Retry(name = "productServiceRetry")
    public boolean updateStock(Long productInfoId, int quantity) {
        return feignClient.updateStock(productInfoId, quantity);
    }

    public boolean updateStockFallback(Long productInfoId, int quantity, Throwable throwable) {
        log.error("[Fallback] 재고 업데이트 실패. productInfoId={}, quantity={}, error={}, errorDetail={}",
                productInfoId, quantity, throwable.getMessage(), throwable);
        return false;
    }
}
