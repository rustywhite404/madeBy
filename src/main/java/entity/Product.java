package entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product extends Timestamped{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String image; //상품 이미지
    private String description; //상품 상세 설명

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean isVisible = true; // 상품 노출 여부 (기본값: true)

    @Column(nullable = false)
    private Long registeredBy; // 상품 등록자 ID (User의 ID 참조)

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductInfo> productInfos = new ArrayList<>();

}
