package com.madeby.cartservice.dto;

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

}