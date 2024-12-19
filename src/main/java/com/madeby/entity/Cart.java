package com.madeby.entity;

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
public class Cart extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cart_id") // 외래 키 설정 (CartProduct 테이블에서 cart_id로 참조)
    private List<CartProduct> cartProducts = new ArrayList<>();

    // 상품 추가
    public void addProduct(Products product, int quantity) {
        CartProduct cartProduct = new CartProduct(product, quantity);
        this.cartProducts.add(cartProduct);
    }

    // 상품 삭제
    public void removeProduct(Products product) {
        this.cartProducts.removeIf(cartProduct -> cartProduct.getProduct().equals(product));
    }

    // 수량 업데이트
    public void updateQuantity(Products product, int quantity) {
        for (CartProduct cartProduct : cartProducts) {
            if (cartProduct.getProduct().equals(product)) {
                cartProduct.setQuantity(quantity);
            }
        }
    }
}
