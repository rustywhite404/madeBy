package com.madeby.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartProductRequestDto {
    private Long productInfoId; //상품 상세 ID
    private int quantity;
}
