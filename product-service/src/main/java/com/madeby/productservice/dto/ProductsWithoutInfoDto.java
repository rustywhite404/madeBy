package com.madeby.productservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class ProductsWithoutInfoDto {
    private Long id;
    private String name;
    private String image;
    private String description;
    private String category;

    public ProductsWithoutInfoDto(Long id, String name, String image, String description, String category) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.description = description;
        this.category = category;
    }
}
