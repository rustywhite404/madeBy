package com.madeby.productservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductsDto {
    private Long id;
    private String name;
    private String image;
    private String description;
    private String category;
    private List<ProductInfoDto> productInfos;
}
