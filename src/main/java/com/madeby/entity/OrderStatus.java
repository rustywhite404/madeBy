package com.madeby.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    ORDERED("주문완료"),
    CANCELED("주문취소"),
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    RETURN_REQUEST("반품신청"),
    RETURNED("반품완료");

    private final String message;
}
