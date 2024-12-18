package com.madeby.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    ORDERED("주문완료"),
    CANCELLED("주문취소"),
    DELIVERED("배송중"),
    RETURN_REQUEST("반품신청"),
    RETURNED("반품완료");

    private final String message;
}
