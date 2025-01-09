package com.madeby.orderservice.client;

import com.madeby.orderservice.dto.ProductInfoDto;
import com.madeby.orderservice.dto.ProductsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceFeignClient {
    @GetMapping("/api/products/{productId}")
    ProductsDto getProduct(@PathVariable("productId") Long productId);

    @GetMapping("/api/products/info/{productInfoId}")
    ProductInfoDto getProductInfo(@PathVariable("productInfoId") Long productInfoId);

    @PostMapping("/api/products/{productInfoId}/decrement-stock")
    boolean decrementStock(@PathVariable("productInfoId") Long productInfoId, @RequestParam("quantity") int quantity);

    @PostMapping("/api/products/{productInfoId}/update-stock")
    boolean updateStock(@PathVariable("productInfoId") Long productInfoId, @RequestParam("quantity") int quantity);
}
