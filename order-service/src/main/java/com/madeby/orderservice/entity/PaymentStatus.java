package com.madeby.orderservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {
    PENDING("결제시도"), PROCESSING("결제중"), COMPLETED("결제완료"), CANCELED("결제취소"), FAILED("결제실패");

    private final String message;
}
