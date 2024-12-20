package com.madeby.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    private String image;
    private String category;
    private Long registeredBy;
    private List<ProductInfoResponseDto> infos;

}