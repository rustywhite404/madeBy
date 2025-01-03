package com.madeby.productservice.util;

import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.repository.ProductInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisInitializer {
    private final RedissonClient redissonClient;
    private final ProductInfoRepository productInfoRepository;

    @PostConstruct
    public void initializeStocks() {
        List<ProductInfo> allProductInfos = productInfoRepository.findAll();
        for (ProductInfo info : allProductInfos) {
            String redisKey = "product_stock:" + info.getId();
            redissonClient.getBucket(redisKey).set(info.getStock());
        }
    }


}
