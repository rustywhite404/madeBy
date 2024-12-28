package com.madeby.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CartResponseDto {
    private Long id; // 장바구니 ID
    private Long userId; // 사용자 ID
    private List<CartProductsDto> products; // 장바구니 상품 목록

    // DTO 내부에 Product ID와 수량만 포함된 구조
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartProductsDto {
        private Long productId; // 상품 ID
        private int quantity; // 장바구니 내 수량
    }
}