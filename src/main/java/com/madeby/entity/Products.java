package com.madeby.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Entity
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
    private boolean isVisible = true;

    @Column(nullable = false)
    @Comment(value = "상품 등록자 ID")
    private Long registeredBy;

    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL)
    private List<ProductInfo> productInfos = new ArrayList<>();

}
