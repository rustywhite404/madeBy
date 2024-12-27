package com.madeby.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderRequestDto {
    private Long productInfoId;
    private int quantity;
}
