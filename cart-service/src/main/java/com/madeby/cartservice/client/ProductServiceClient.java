package com.madeby.cartservice.client;

import com.madeby.cartservice.dto.ProductInfoDto;
import com.madeby.cartservice.dto.ProductsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}")
    ProductsDto getProductById(@PathVariable Long productId);

    @GetMapping("/api/products/info/{productInfoId}")
    ProductInfoDto getProductInfoById(@PathVariable("productInfoId") Long productInfoId);

}