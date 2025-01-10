package com.madeby.productservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Entity
@Table(name = "product_info", indexes = {
        @Index(name = "idx_product_info_stock", columnList = "id,stock"),
        @Index(name = "idx_product_info_visible", columnList = "is_visible")
})
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

    @Column(nullable = false)
    @Comment(value = "한정 상품 여부")
    @Builder.Default
    private boolean isLimited = false;

    @Column(nullable = true)
    @Comment(value = "해당 옵션 노출 여부")
    @Builder.Default
    private boolean isVisible = true;

}