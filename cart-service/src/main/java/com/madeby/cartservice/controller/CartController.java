package com.madeby.cartservice.controller;

import com.madeBy.shared.common.ApiResponse;
import com.madeby.cartservice.dto.CartProductRequestDto;
import com.madeby.cartservice.dto.CartRequestDto;
import com.madeby.cartservice.dto.CartResponseDto;
import com.madeby.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public CartResponseDto getCartByUserId(@PathVariable Long userId) {
        return cartService.getCartByUserId(userId);
    }

    @GetMapping("/remove")
    public void removeProductFromCart(@RequestParam Long userId, @RequestParam Long productInfoId) {
        cartService.removeProductFromCart(userId, productInfoId);
    }

    @GetMapping("/clear")
    public void clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
    }

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<Object> getCart(Long userId) {
        CartResponseDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }


    // 장바구니에 상품 추가
    @PostMapping("/add")
    public ResponseEntity<Object> addProduct(
            Long userId,
            @RequestBody CartRequestDto request
    ) {
        // productInfoId와 quantity가 null인 경우 로그 확인
        if (request.getProductInfoId() == null || request.getQuantity() <= 0) {
            log.error("Invalid request: productInfoId={}, quantity={}", request.getProductInfoId(), request.getQuantity());
            throw new IllegalArgumentException("Invalid request data");
        }

        cartService.addProduct(userId, request.getProductInfoId(), request.getQuantity()); // productInfoId 사용
        return ResponseEntity.ok(ApiResponse.success("상품이 장바구니에 추가되었습니다."));
    }

    // 장바구니에서 상품 삭제
    @DeleteMapping("/remove")
    public ResponseEntity<Object> removeProduct(
            Long userId,
            @RequestParam Long productInfoId
    ) {
        log.info("장바구니 상품 삭제 요청 - userId: {}, productInfoId: {}", userId, productInfoId);

        cartService.removeProduct(userId, productInfoId); // productInfoId 사용
        CartResponseDto cart = cartService.getCart(userId); // 변경 후 장바구니 상태 조회
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    // 장바구니 상품 수량 업데이트
    @PatchMapping("/update")
    public ResponseEntity<Object> updateProductQuantity(
            Long userId,
            @RequestBody CartProductRequestDto cartProductRequest
    ) {
        log.info("장바구니 상품 수량 업데이트 요청 - userId: {}, productInfoId: {}, quantity: {}",
                userId, cartProductRequest.getProductInfoId(), cartProductRequest.getQuantity());

        cartService.updateProductQuantity(userId, cartProductRequest.getProductInfoId(), cartProductRequest.getQuantity()); // productInfoId 사용
        CartResponseDto cart = cartService.getCart(userId); // 변경 후 장바구니 상태 조회
        return ResponseEntity.ok(ApiResponse.success(cart));
    }


}
