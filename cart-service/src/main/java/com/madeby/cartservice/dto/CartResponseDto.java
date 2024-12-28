package com.madeby.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.madeby.cartservice.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 필드 무시
public class CartResponseDto {
    private Long id;
    private Long userId; // 유저 ID만 포함
    private List<CartProductsDto> products; // CartProductsDto로 변경

    // Entity -> DTO 변환 메서드
    public static CartResponseDto fromEntity(Cart cart) {
        return CartResponseDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId()) // 유저 ID만 추출
                .products(cart.getCartProducts().stream()
                        .map(CartProductsDto::fromEntity) // CartProductsDto로 변환
                        .collect(Collectors.toList()))
                .build();
    }
}
