package com.madeby.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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

}