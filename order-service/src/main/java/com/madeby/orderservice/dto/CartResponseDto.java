package com.madeby.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 필드 무시
public class CartResponseDto {
    private Long id; // 장바구니 ID
    private Long userId; // 사용자 ID
    private List<CartProductsDto> products; // 장바구니 상품 목록
}