package com.madeby.payservice.entity;

import com.madeBy.shared.entity.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Comment("결제 상태")
    private PaymentStatus status;

    @Comment("결제 완료 시간")
    private LocalDateTime completedAt;
}

