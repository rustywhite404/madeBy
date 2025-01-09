package com.madeby.productservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeby.productservice.client.NaverApiClient;
import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.Products;
import com.madeby.productservice.repository.ProductsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class ProductsInitializer {
    private final ProductsRepository productsRepository;
    private final NaverApiClient naverApiClient;

    private static final int ITEMS_PER_REQUEST = 100; // 한 번의 요청당 가져올 상품 수
    private static final List<String> KEYWORDS = List.of(
            // 더미 키워드 예시
            "아웃도어용배낭", "클라이밍카라비너", "등산지도", "하이킹용스틱", "캠핑화로대",
            "낚시의자", "폴딩카트", "스노클세트", "야외용텐트", "라운지체어",
            "조립식텐트", "그늘막", "캠핑수납박스", "캠핑테이블웨어", "다용도랜턴"
    );

    private static final List<String> COLORS = List.of("Red", "Blue", "Green", "Black", "White");
    private static final List<String> SIZES = List.of("S", "M", "L", "XL");

    private final Random random = new Random();

    @PostConstruct
    public void initializeProducts() {
        if (productsRepository.count() > 0) {
            return; // 이미 데이터가 있다면 초기화하지 않음
        }

        ObjectMapper objectMapper = new ObjectMapper();

        for (String keyword : KEYWORDS) {
            try {
                // 네이버 API 호출
                String response = naverApiClient.searchProducts(keyword, 1, ITEMS_PER_REQUEST);
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode items = rootNode.path("items");

                if (items.isEmpty()) {
                    System.out.println("키워드 [" + keyword + "]로 데이터가 없습니다.");
                    continue; // 데이터가 없으면 다음 키워드로 진행
                }

                // 가져온 데이터를 저장할 리스트 생성
                List<Products> productsList = new ArrayList<>();
                for (JsonNode item : items) {
                    Products product = Products.builder()
                            .name(item.path("title").asText().replaceAll("<[^>]*>", "")) // HTML 태그 제거
                            .category(keyword)
                            .description(item.path("description").asText())
                            .image(item.path("image").asText())
                            .isVisible(true)
                            .build();

                    // 임의의 ProductInfo 생성
                    List<ProductInfo> productInfos = new ArrayList<>();
                    for (int j = 1; j <= 2; j++) {
                        ProductInfo info = ProductInfo.builder()
                                .products(product)
                                .price(BigDecimal.valueOf(50000 + random.nextInt(450000))) // 50000 ~ 500000 사이 랜덤 가격
                                .stock(100 + random.nextInt(50)) // 100 ~ 150 랜덤 재고
                                .size(SIZES.get(random.nextInt(SIZES.size()))) // 랜덤 사이즈
                                .color(COLORS.get(random.nextInt(COLORS.size()))) // 랜덤 컬러
                                .isLimited(false)
                                .build();
                        productInfos.add(info);
                    }

                    product.setProductInfos(productInfos);
                    productsList.add(product);
                }

                // 키워드에 대한 데이터 저장
                productsRepository.saveAll(productsList);
                System.out.println("키워드 [" + keyword + "]로 100개의 데이터를 저장했습니다.");

            } catch (Exception e) {
                System.err.println("키워드 [" + keyword + "] 처리 중 오류 발생: " + e.getMessage());
            }
        }
    }
}
