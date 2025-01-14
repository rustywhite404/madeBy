package com.madeby.payservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductSnapshotDto {

    private Long productInfoId; // 원본 상품 ID
    private int stock; // 재고
    private String size; // 사이즈
    private String color; // 색상
    private int quantity; // 수량
    private BigDecimal price; // 가격
    private BigDecimal totalAmount; // 총액

}
