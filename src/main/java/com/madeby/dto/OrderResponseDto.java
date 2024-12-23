package com.madeby.dto;

import com.madeby.entity.OrderProductSnapshot;
import com.madeby.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {

    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
    private boolean isReturnable;
    private List<OrderProductSnapshotDto> products;

    public static OrderResponseDto fromEntity(Orders order) {
        // Null-safe snapshot 처리
        List<OrderProductSnapshot> snapshots = Optional.ofNullable(order.getOrderProductSnapshots())
                .orElse(Collections.emptyList());

        // TotalAmount 계산
        BigDecimal totalAmount = snapshots.stream()
                .map(OrderProductSnapshot::getTotalAmount)
                .filter(Objects::nonNull) // Null 값 필터링
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 원하는 시간대로 변환 후 LocalDate 가져오기
        ZoneId zoneId = ZoneId.of("Asia/Seoul"); // 시간대 설정

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalAmount(totalAmount)
                .orderDate(order.getCreatedAt().toLocalDate())
                .isReturnable(order.isReturnable())
                .products(snapshots.stream()
                        .map(OrderProductSnapshotDto::fromEntity)
                        .toList())
                .build();
    }
}