package com.madeby.productservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfoDocument {
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private int stock;

    @Field(type = FieldType.Keyword)
    private String size;

    @Field(type = FieldType.Keyword)
    private String color;

    @Field(type = FieldType.Boolean)
    private boolean isLimited;

    @Field(type = FieldType.Boolean)
    private boolean isVisible;
}
