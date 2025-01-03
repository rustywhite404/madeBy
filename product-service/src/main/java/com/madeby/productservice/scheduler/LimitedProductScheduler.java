package com.madeby.productservice.scheduler;

import com.madeby.productservice.service.ProductsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitedProductScheduler {

    private final ProductsService productsService;

    @Scheduled(cron = "0 0 14 * * ?") // 매일 오후 2시 실행
    public void scheduleLimitedProductsVisibilityUpdate() {
        productsService.updateLimitedProductsVisibility();
    }
}
