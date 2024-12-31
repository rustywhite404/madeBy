package com.madeby.cartservice.entity;

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

    @Column(nullable = false)
    private Long productInfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    @Comment(value = "담은 수량")
    private int quantity;

    public CartProduct(Cart cart, Long productInfoIdId, int quantity) {
        this.cart = cart;
        this.productInfoId = productInfoIdId;
        this.quantity = quantity;
    }
}
