package com.madeby.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_orders_user_date", columnList = "user_id, created_at")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Orders extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Comment("주문자 ID")
    private Long userId; // User 엔티티 직접 참조 제거

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment(value = "주문 상품 스냅샷 목록")
    @Builder.Default
    private List<OrderProductSnapshot> orderProductSnapshots = new ArrayList<>();

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Comment(value = "주문상태")
    private OrderStatus status;

    @Comment(value = "배송 시작일")
    private LocalDateTime deliveryStartDate;

    @Comment(value = "배송 완료일")
    private LocalDateTime deliveryEndDate;

    @Comment(value = "반품 신청일")
    private LocalDateTime returnRequestedDate;

    @Column(nullable = false)
    @Comment(value = "반품 가능 여부")
    private boolean isReturnable;

    public Orders(Long userId, OrderStatus status, boolean isReturnable) {
        this.userId = userId;
        this.status = status;
        this.isReturnable = isReturnable;
        this.orderProductSnapshots = new ArrayList<>(); // 컬렉션 초기화
    }
}
