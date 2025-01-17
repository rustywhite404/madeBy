package com.madeby.cartservice.controller;

import com.madeBy.shared.common.ApiResponse;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.cartservice.client.UserServiceClient;
import com.madeby.cartservice.dto.CartProductRequestDto;
import com.madeby.cartservice.dto.CartRequestDto;
import com.madeby.cartservice.dto.CartResponseDto;
import com.madeby.cartservice.dto.UserDetailsDto;
import com.madeby.cartservice.service.CartService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserServiceClient userServiceClient;

    @GetMapping("/{userId}")
    public CartResponseDto getCartByUserId(@PathVariable Long userId) {
        return cartService.getCartByUserId(userId);
    }

    // 장바구니에서 상품 삭제
    @DeleteMapping("/remove")
    public ResponseEntity<Object> removeProduct(@RequestHeader("X-User-Id") Long userId,
                                                @RequestParam Long productInfoId
    ) {
        log.info("장바구니 상품 삭제 요청 - userId: {}, productInfoId: {}", userId, productInfoId);

        cartService.removeProduct(userId, productInfoId); // productInfoId 사용
        CartResponseDto cart = cartService.getCart(userId); // 변경 후 장바구니 상태 조회
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @GetMapping("/clear")
    public void clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
    }

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<Object> getCart(@RequestHeader("X-User-Id") Long userId) {
        CartResponseDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    // 장바구니에 상품 추가
    @PostMapping("/add")
    public ResponseEntity<Object> addProduct(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Enabled") boolean isEnabled,
            @RequestBody CartRequestDto request
    ) {
        log.info("Received User ID: {}, Role: {}, Enabled: {}", userId, role, isEnabled);

        if (!isEnabled) {
            throw new IllegalArgumentException("비활성화된 사용자입니다.");
        }

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효한 user-id가 필요합니다.");
        }
        try {
            // 요청 처리
            cartService.addProduct(userId, request.getProductInfoId(), request.getQuantity());
            return ResponseEntity.ok(ApiResponse.success("상품이 장바구니에 추가되었습니다."));
        } catch (MadeByException ex) {
            log.error("상품을 장바구니에 추가하던 중 에러가 발생했습니다: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.failure(ex.getMadeByErrorCode().name(),ex.getMessage()));
        }

    }


    // 장바구니 상품 수량 업데이트
    @PatchMapping("/update")
    public ResponseEntity<Object> updateProductQuantity(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Enabled") boolean isEnabled,
            @RequestBody CartProductRequestDto cartProductRequest
    ) {
        log.info("장바구니 상품 수량 업데이트 요청 - userId: {}, productInfoId: {}, quantity: {}",
                userId, cartProductRequest.getProductInfoId(), cartProductRequest.getQuantity());

        cartService.updateProductQuantity(userId, cartProductRequest.getProductInfoId(), cartProductRequest.getQuantity()); // productInfoId 사용
        CartResponseDto cart = cartService.getCart(userId); // 변경 후 장바구니 상태 조회
        return ResponseEntity.ok(ApiResponse.success(cart));
    }


}
