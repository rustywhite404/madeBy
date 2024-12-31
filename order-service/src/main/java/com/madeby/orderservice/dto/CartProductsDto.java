package com.madeby.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CartProductsDto {
    private Long productInfoId; // 상품 정보 ID
    private int quantity; // 수량
}