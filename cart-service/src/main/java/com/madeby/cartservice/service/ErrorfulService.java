package com.madeby.cartservice.service;

import com.madeby.cartservice.client.ProductServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ErrorfulService {

    private final ProductServiceClient productServiceClient;

    // Circuit Breaker와 Retry 적용
    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackForCase1")
    @Retry(name = "myCBRetry")
    public String handleCase1() {
        return productServiceClient.callCase1();
    }

    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackForCase2")
    @Retry(name = "myCBRetry")
    public String handleCase2() {
        return productServiceClient.callCase2();
    }

    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackForCase3")
    @Retry(name = "myCBRetry")
    public String handleCase3() {
        return productServiceClient.callCase3();
    }

    // Fallback 메서드
    private String fallbackForCase1(Throwable throwable) {
        log.info("----Fallback response for case1");
        return "Fallback response for case1";
    }

    private String fallbackForCase2(Throwable throwable) {
        log.info("----Fallback response for case2");
        return "Fallback response for case2";
    }

    private String fallbackForCase3(Throwable throwable) {
        log.info("----Fallback response for case3");
        return "Fallback response for case3";
    }
}
