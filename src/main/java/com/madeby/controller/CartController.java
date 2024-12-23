package com.madeby.controller;

import com.madeby.dto.CartProductRequestDto;
import com.madeby.dto.CartRequestDto;
import com.madeby.dto.CartResponseDto;
import com.madeby.security.UserDetailsImpl;
import com.madeby.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        CartResponseDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }


    // 장바구니에 상품 추가
    @PostMapping("/add")
    public ResponseEntity<String> addProduct(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CartRequestDto request
    ) {
        Long userId = userDetails.getUser().getId();
        // productInfoId와 quantity가 null인 경우 로그 확인
        if (request.getProductInfoId() == null || request.getQuantity() <= 0) {
            log.error("Invalid request: productInfoId={}, quantity={}", request.getProductInfoId(), request.getQuantity());
            throw new IllegalArgumentException("Invalid request data");
        }

        cartService.addProduct(userId, request.getProductInfoId(), request.getQuantity()); // productInfoId 사용
        return ResponseEntity.ok("상품이 장바구니에 추가되었습니다.");
    }

    // 장바구니에서 상품 삭제
    @DeleteMapping("/remove")
    public ResponseEntity<Object> removeProduct(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam Long productInfoId
    ) {
        Long userId = userDetails.getUser().getId();
        log.info("장바구니 상품 삭제 요청 - userId: {}, productInfoId: {}", userId, productInfoId);

        cartService.removeProduct(userId, productInfoId); // productInfoId 사용
        CartResponseDto cart = cartService.getCart(userId); // 변경 후 장바구니 상태 조회
        return ResponseEntity.ok(cart);
    }

    // 장바구니 상품 수량 업데이트
    @PatchMapping("/update")
    public ResponseEntity<Object> updateProductQuantity(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody CartProductRequestDto cartProductRequest
    ) {
        Long userId = userDetails.getUser().getId();
        log.info("장바구니 상품 수량 업데이트 요청 - userId: {}, productInfoId: {}, quantity: {}",
                userId, cartProductRequest.getProductInfoId(), cartProductRequest.getQuantity());

        cartService.updateProductQuantity(userId, cartProductRequest.getProductInfoId(), cartProductRequest.getQuantity()); // productInfoId 사용
        CartResponseDto cart = cartService.getCart(userId); // 변경 후 장바구니 상태 조회
        return ResponseEntity.ok(cart);
    }


}
