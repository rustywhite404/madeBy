package com.madeby.cartservice.client;

import com.madeBy.shared.common.ApiResponse;
import com.madeby.cartservice.dto.ProductInfoDto;
import com.madeby.cartservice.dto.ProductsDto;
import com.madeby.cartservice.fallback.ProductServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "product-service", fallback = ProductServiceFallback.class)
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}")
    ProductsDto getProductById(@PathVariable Long productId);

    @GetMapping("/api/products/info/{productInfoId}")
    ProductInfoDto getProductInfoById(@PathVariable("productInfoId") Long productInfoId);

}