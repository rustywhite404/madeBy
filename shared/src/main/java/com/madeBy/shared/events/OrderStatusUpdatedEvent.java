package com.madeBy.shared.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdatedEvent {
    private Long orderId;
    private String status;
    private Long productInfoId;
    private int quantity;
}
