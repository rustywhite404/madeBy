package com.madeby.userservice.dto;

import com.madeby.userservice.entity.OrderProductSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductSnapshotDto {

    private String productName;
    private String productImage;
    private String productDescription;
    private String category;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;

    public static OrderProductSnapshotDto fromEntity(OrderProductSnapshot snapshot) {
        return OrderProductSnapshotDto.builder()
                .productName(snapshot.getProductName())
                .productImage(snapshot.getProductImage())
                .productDescription(snapshot.getProductDescription())
                .category(snapshot.getCategory())
                .quantity(snapshot.getQuantity())
                .price(snapshot.getPrice())
                .totalAmount(snapshot.getTotalAmount())
                .build();
    }
}