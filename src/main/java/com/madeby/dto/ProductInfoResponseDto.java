package com.madeby.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductInfoResponseDto {
    private Long id;      // ProductInfo ID
    private double price; // 상품 가격
    private int stock;    // 재고 수량
    private String size;  // 사이즈
    private String color; // 색상
}