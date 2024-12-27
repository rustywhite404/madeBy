package com.madeby.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartRequestDto {
    private Long productInfoId; // 상품 상세 ID
    private int quantity;       // 수량
}
