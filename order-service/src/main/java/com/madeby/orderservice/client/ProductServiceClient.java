package com.madeby.orderservice.client;

import com.madeby.orderservice.dto.ProductInfoDto;
import com.madeby.orderservice.dto.ProductsDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceClient {

    private final ProductServiceFeignClient feignClient;
    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "getProductFallback")
    @Retry(name = "productServiceRetry")
    public ProductsDto getProduct(Long productId) {
        return feignClient.getProduct(productId);
    }

    public ProductsDto getProductFallback(Long productId, Throwable throwable) {
        log.error("[Fallback] 제품 정보 가져오기 실패. productId={}, error={}, errorDetail={}",
                productId, throwable.getMessage(), throwable);
        throw new RuntimeException("상품 서비스 일시적 장애: " + throwable.getMessage());
    }

    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "getProductInfoFallback")
    @Retry(name = "productServiceRetry")
    public ProductInfoDto getProductInfo(Long productInfoId) {
        return feignClient.getProductInfo(productInfoId);
    }

    public ProductInfoDto getProductInfoFallback(Long productInfoId, Throwable throwable) {
        log.error("[Fallback] 제품 상세 정보 가져오기 실패. productInfoId={}, error={}, errorDetail={}",
                productInfoId, throwable.getMessage(), throwable);
        throw new RuntimeException("상품 정보 서비스 일시적 장애: " + throwable.getMessage());
    }

    @CircuitBreaker(name = "productServiceCircuitBreaker", fallbackMethod = "decrementStockFallback")
    @Retry(name = "productServiceRetry")
    public boolean decrementStock(Long productInfoId, int quantity) {
        boolean result = feignClient.decrementStock(productInfoId, quantity);
        return result;
    }

    public boolean decrementStockFallback(Long productInfoId, int quantity, Throwable throwable) {
        log.error("[Fallback] 재고 차감 실패. productInfoId={}, quantity={}, error={}, errorDetail={}",
                productInfoId, quantity, throwable.getMessage(), throwable);
        return false; // 실패 시 false 반환
    }

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
