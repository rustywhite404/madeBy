package com.madeby.payservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {
    private Long productInfoId;
    private int quantity;
}
