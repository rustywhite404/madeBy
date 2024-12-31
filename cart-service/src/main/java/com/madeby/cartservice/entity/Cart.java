package com.madeby.cartservice.entity;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Cart extends Timestamped{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartProduct> cartProducts = new ArrayList<>();

    // 상품 추가
    public void addProduct(Long productInfoId, int quantity) {
        for (CartProduct cartProduct : cartProducts) {
            if (cartProduct.getProductInfoId().equals(productInfoId)) {
                cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
                return; // 수량 업데이트
            }
        }
        this.cartProducts.add(new CartProduct(this, productInfoId, quantity));
    }

    // 상품 삭제
    public void removeProduct(Long productInfoId) {
        boolean removed = this.cartProducts.removeIf(product ->
                product.getProductInfoId().equals(productInfoId));
        if (!removed) {
            throw new MadeByException(MadeByErrorCode.CART_PRODUCT_NOT_FOUND);
        }
    }

    // 수량 업데이트
    public void updateQuantity(Long productId, int quantity) {
        for (CartProduct cartProduct : cartProducts) {
            if (cartProduct.getProductInfoId().equals(productId)) {
                cartProduct.setQuantity(quantity);
                return;
            }
        }
        throw new MadeByException(MadeByErrorCode.CART_PRODUCT_NOT_FOUND);
    }

}