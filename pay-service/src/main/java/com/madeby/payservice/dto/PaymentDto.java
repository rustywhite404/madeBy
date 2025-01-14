package com.madeby.payservice.dto;
import com.madeBy.shared.entity.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private Long orderId;
    private PaymentStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}