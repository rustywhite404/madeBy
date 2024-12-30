package com.madeby.productservice.dto;

import com.madeby.productservice.entity.ProductInfo;
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

    public static ProductInfoDto fromEntity(ProductInfo productInfo) {
        return ProductInfoDto.builder()
                .id(productInfo.getId())
                .price(productInfo.getPrice())
                .stock(productInfo.getStock())
                .size(productInfo.getSize())
                .color(productInfo.getColor())
                .build();
    }
}