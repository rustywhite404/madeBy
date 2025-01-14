package com.madeby.payservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductInfoDto {
    private Long id;
    private BigDecimal price;
    private int stock;
    private String size;
    private String color;
    private Long productId;
    private boolean isLimited;
    private boolean isVisible;
}