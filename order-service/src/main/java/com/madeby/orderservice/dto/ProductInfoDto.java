package com.madeby.orderservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProductInfoDto {
    private Long id;
    private BigDecimal price;
    private int stock;
    private String size;
    private String color;
    private Long productId;
}