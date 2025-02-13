package com.madeby.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_order_product_info", columnList = "order_id, product_info_id"),
        @Index(name = "idx_product_info", columnList = "product_info_id")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 아래 데이터들은 모두 스냅샷 당시의 상품 정보. 현재 상품 정보와 상이할 수 있음.
     * */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @Comment(value = "해당 스냅샷이 속한 주문 ID")
    private Orders orders;

    @Column(nullable = false)
    @Comment(value = "원본 상품 ID")
    private Long productInfoId;

    private int stock;
    private String size;
    private String color;

    @Column(nullable = false)
    @Comment(value = "주문된 각 상품의 수량")
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    @Comment(value = "상품 가격")
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    @Comment(value = "상품 총액 (수량 * 가격)")
    private BigDecimal totalAmount;

    public OrderProductSnapshot(
            Orders orders,
            Long productInfoId,
            int stock,
            String size,
            String color,
            int quantity,
            BigDecimal price,
            BigDecimal totalAmount) {
        this.orders = orders;
        this.productInfoId = productInfoId;
        this.stock = stock;
        this.size = size;
        this.color = color;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
    }

    @PrePersist //totalAmount 자동계산
    public void calculateTotalAmount() {
        this.totalAmount = this.price.multiply(BigDecimal.valueOf(this.quantity));
    }
}