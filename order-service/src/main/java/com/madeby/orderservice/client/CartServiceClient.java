package com.madeby.orderservice.client;
import com.madeby.orderservice.dto.CartResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service")
public interface CartServiceClient {

    @GetMapping("/api/cart/{userId}")
    CartResponseDto getCartByUserId(@PathVariable("userId") Long userId);

    @DeleteMapping("/api/cart/remove")
    void removeProductFromCart(
            @RequestHeader("X-User-Id") Long userId, // 헤더로 userId 전달
            @RequestParam("productInfoId") Long productInfoId // 쿼리 파라미터로 productInfoId 전달
    );


    @GetMapping("/api/cart/clear")
    void clearCart(@RequestParam("userId") Long userId);
}