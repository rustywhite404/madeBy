package com.madeby.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 연관된 상품

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private int stock; //재고 수량

    @Column(nullable = false)
    private String size; // 상품 사이즈

    @Column(nullable = false)
    private String color; // 상품 색상
}