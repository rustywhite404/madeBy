package com.madeby.productservice.util;

import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.Products;
import com.madeby.productservice.repository.ProductsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductsInitializer {

    private final ProductsRepository productsRepository;

    @PostConstruct
    public void initializeProducts() {
        if (productsRepository.count() > 0) {
            return; // 이미 데이터가 있다면 초기화하지 않음
        }

        List<Products> productsList = new ArrayList<>();

        // 일반 상품 생성
        for (int i = 1; i <= 10; i++) {
            Products product = Products.builder()
                    .name("General Product " + i)
                    .category("Category " + ((i % 3) + 1)) // 3개의 카테고리 순환
                    .description("Description for General Product " + i)
                    .image("general_image" + i + ".jpg")
                    .isVisible(true) // 일반 상품은 기본적으로 보임
                    .build();

            List<ProductInfo> productInfos = new ArrayList<>();
            for (int j = 1; j <= 3; j++) { // 각 상품에 대해 3개의 옵션 생성
                ProductInfo info = ProductInfo.builder()
                        .products(product)
                        .price(BigDecimal.valueOf(1000 + (j * 100) + (i * 10))) // 가격
                        .stock(50 + (i * 5)) // 재고
                        .size("Size " + j) // 사이즈
                        .color("Color " + j) // 색상
                        .isLimited(false) // 일반 상품 옵션
                        .build();

                productInfos.add(info);
            }

            product.setProductInfos(productInfos);
            productsList.add(product);
        }

        // 한정 상품 생성
        for (int i = 1; i <= 5; i++) {
            Products product = Products.builder()
                    .name("Limited Product " + i)
                    .category("Limited Category " + ((i % 2) + 1)) // 2개의 카테고리 순환
                    .description("Description for Limited Product " + i)
                    .image("limited_image" + i + ".jpg")
                    .build();

            List<ProductInfo> productInfos = new ArrayList<>();
            for (int j = 1; j <= 2; j++) { // 각 한정 상품에 대해 2개의 옵션 생성
                ProductInfo info = ProductInfo.builder()
                        .products(product)
                        .price(BigDecimal.valueOf(2000 + (j * 150) + (i * 20))) // 가격
                        .stock(10 + (i * 2)) // 재고
                        .size("Limited Size " + j) // 사이즈
                        .color("Limited Color " + j) // 색상
                        .isLimited(true) // 한정 상품 옵션
                        .isVisible(false) // 한정 상품은 기본적으로 보이지 않음
                        .build();

                productInfos.add(info);
            }

            product.setProductInfos(productInfos);
            productsList.add(product);
        }

        productsRepository.saveAll(productsList);
    }
}
