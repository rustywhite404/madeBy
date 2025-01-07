package com.madeby.productservice.scheduler;

import com.madeby.productservice.service.ProductsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LimitedProductScheduler {

    private final ProductsService productsService;

    @Scheduled(cron = "0 27 14 * * ?") // 매일 오후 2시 실행

    public void scheduleLimitedProductsVisibilityUpdate() {
        log.info("[한정상품 공개]--------스케줄러 실행");
        productsService.updateLimitedProductsVisibility();
    }
}
