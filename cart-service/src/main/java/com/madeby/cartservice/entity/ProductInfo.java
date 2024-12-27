package com.madeby.cartservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

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
    @JsonIgnore
    @JsonBackReference // 부모 참조
    private Products products; // 연관된 상품

    @Column(nullable = false)
    @Comment(value = "상품가격")
    private BigDecimal price;

    @Column(nullable = false)
    @Comment(value = "재고수량")
    private int stock;

    @Column(nullable = false)
    @Comment(value = "사이즈")
    private String size;

    @Column(nullable = false)
    @Comment(value = "색상")
    private String color;
}