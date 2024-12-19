package com.madeby.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Orders extends Timestamped{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private List<OrderProducts> orderProducts = new ArrayList<>();

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Comment(value = "주문상태")
    private OrderStatus status;

    @Comment(value = "배송 시작일")
    private LocalDate deliveryStartDate;

    @Comment(value = "반품 신청일")
    private LocalDate returnRequestedDate;

    @Column(nullable = false)
    @Comment(value = "반품 가능 여부")
    private boolean isReturnable;

}
