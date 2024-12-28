package com.madeby.orderservice.client;
import com.madeby.orderservice.dto.CartResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cart-service")
public interface CartServiceClient {

    @GetMapping("/api/cart/{userId}")
    CartResponseDto getCartByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/api/cart/remove")
    void removeProductFromCart(@RequestParam("userId") Long userId, @RequestParam("productInfoId") Long productInfoId);

    @GetMapping("/api/cart/clear")
    void clearCart(@RequestParam("userId") Long userId);
}