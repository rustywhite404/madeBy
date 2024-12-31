package com.madeby.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartProductRequestDto {
    private Long productInfoId; //상품 상세 ID
    private int quantity;
}
