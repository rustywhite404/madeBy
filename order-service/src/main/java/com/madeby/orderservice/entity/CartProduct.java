package com.madeby.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_info_id", nullable = false) // ProductInfo와 연관
    private ProductInfo productInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    @Comment(value = "담은 수량")
    private int quantity;

    public CartProduct(Cart cart, ProductInfo productInfo, int quantity) {
        this.cart = cart;
        this.productInfo = productInfo;
        this.quantity = quantity;
    }
}
