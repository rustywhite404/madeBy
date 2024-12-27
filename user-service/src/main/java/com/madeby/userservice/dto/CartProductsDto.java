package com.madeby.userservice.dto;

import com.madeby.userservice.entity.CartProduct;
import com.madeby.userservice.entity.ProductInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartProductsDto {
    private Long productInfoId; // ProductInfo ID
    private String productName; // Product Name
    private int quantity;
    private BigDecimal price;
    private String size;
    private String color;

    // Entity -> DTO 변환 메서드
    public static CartProductsDto fromEntity(CartProduct cartProduct) {
        ProductInfo productInfo = cartProduct.getProductInfo();
        return CartProductsDto.builder()
                .productInfoId(productInfo.getId())
                .productName(productInfo.getProducts().getName()) // Product name 추가
                .quantity(cartProduct.getQuantity())
                .price(productInfo.getPrice())
                .size(productInfo.getSize())
                .color(productInfo.getColor())
                .build();
    }
}
