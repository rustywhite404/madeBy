package com.madeby.payservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceFeignClient {
    @PostMapping("/api/products/{productInfoId}/update-stock")
    boolean updateStock(@PathVariable("productInfoId") Long productInfoId, @RequestParam("quantity") int quantity);
}
