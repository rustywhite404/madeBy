package com.madeby.cartservice.dto;

import com.madeby.cartservice.entity.CartProduct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CartProductsDto {
    private Long productId;
    private int quantity;

    // Entity -> DTO 변환
    public static CartProductsDto fromEntity(CartProduct cartProduct) {
        return new CartProductsDto(
                cartProduct.getProductId(),
                cartProduct.getQuantity()
        );
    }
}
