package com.madeby.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.ArrayList;
import java.util.List;

@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/es-settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String image;

    @Field(type = FieldType.Boolean)
    private boolean isVisible;

    @Field(type = FieldType.Nested)
    private List<ProductInfoDocument> productInfos = new ArrayList<>();
}