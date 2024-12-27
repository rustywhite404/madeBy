package com.madeby.productservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfoDto {
    private Long id;
    private BigDecimal price;
    private int stock;
    private String size;
    private String color;
}