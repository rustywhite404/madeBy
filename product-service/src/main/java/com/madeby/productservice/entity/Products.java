package com.madeby.productservice.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_name_visible", columnList = "name,is_visible"),
        @Index(name = "idx_products_visible_id", columnList = "is_visible,id")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Products extends Timestamped{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Comment(value = "상품명")
    private String name;

    @Comment(value = "상품이미지")
    private String image;

    @Comment(value = "상세설명")
    private String description;

    @Comment(value = "카테고리")
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    @Comment(value = "상품 노출여부")
    @Builder.Default
    private boolean isVisible = true;

    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // 자식 참조
    @Builder.Default
    private List<ProductInfo> productInfos = new ArrayList<>();


}
