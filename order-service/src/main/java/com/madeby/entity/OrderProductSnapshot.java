package com.madeby.entity;

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
public class OrderProductSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @Comment(value = "해당 스냅샷이 속한 주문 ID")
    private Orders orders;

    @Column(nullable = false)
    @Comment(value = "원본 상품 ID")
    private Long productId;

    /*
    * 아래 데이터들은 모두 스냅샷 당시의 상품 정보. 현재 상품 정보와 상이할 수 있음.
    * */
    @Column(nullable = false)
    @Comment(value = "상품명")
    private String productName;

    @Column(nullable = true)
    @Comment(value = "상품 이미지 URL")
    private String productImage;

    @Column(nullable = true)
    @Comment(value = "상품 상세설명")
    private String productDescription;

    @Column(nullable = false)
    @Comment(value = "상품 카테고리")
    private String category;

    @Column(nullable = false)
    @Comment(value = "주문된 각 상품의 수량")
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    @Comment(value = "상품 가격")
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    @Comment(value = "상품 총액 (수량 * 가격)")
    private BigDecimal totalAmount;

    @PrePersist //totalAmount 자동계산
    public void calculateTotalAmount() {
        this.totalAmount = this.price.multiply(BigDecimal.valueOf(this.quantity));
    }
}