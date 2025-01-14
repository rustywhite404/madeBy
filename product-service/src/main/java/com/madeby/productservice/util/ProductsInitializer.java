package com.madeby.productservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeby.productservice.client.NaverApiClient;
import com.madeby.productservice.dto.ProductInfoDto;
import com.madeby.productservice.dto.ProductsDto;
import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.Products;
import com.madeby.productservice.repository.ProductsRepository;
import com.madeby.productservice.service.ProductsService;
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
    private final ProductsService productsService;
    private final ProductsRepository productsRepository;
    private final NaverApiClient naverApiClient;

    private static final int ITEMS_PER_REQUEST = 100; // 한 번의 요청당 가져올 상품 수
    private static final List<String> KEYWORDS = List.of(
            // 더미 키워드 예시
            "모자", "우산", "스니커즈", "스누피", "가방"
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

                // 가져온 데이터를 저장
                for (JsonNode item : items) {
                    ProductsDto productsDto = ProductsDto.builder()
                            .name(item.path("title").asText().replaceAll("<[^>]*>", "")) // HTML 태그 제거
                            .category(keyword)
                            .description("")
                            .image(item.path("image").asText())
                            .productInfos(generateProductInfos())
                            .build();

                    productsService.registerNewProduct(productsDto);
                }

                System.out.println("키워드 [" + keyword + "]로 100개의 데이터를 저장했습니다.");

                // 1초 대기
                Thread.sleep(1000);

            } catch (Exception e) {
                System.err.println("키워드 [" + keyword + "] 처리 중 오류 발생: " + e.getMessage());
            }
        }
    }

    private List<ProductInfoDto> generateProductInfos() {
        List<ProductInfoDto> productInfos = new ArrayList<>();

        for (int j = 1; j <= 2; j++) {
            ProductInfoDto infoDto = ProductInfoDto.builder()
                    .price(BigDecimal.valueOf(50000 + random.nextInt(450000))) // 50000 ~ 500000 사이 랜덤 가격
                    .stock(100 + random.nextInt(50)) // 100 ~ 150 랜덤 재고
                    .size(SIZES.get(random.nextInt(SIZES.size()))) // 랜덤 사이즈
                    .color(COLORS.get(random.nextInt(COLORS.size()))) // 랜덤 컬러
                    .isLimited(false)
                    .isVisible(true)
                    .build();

            productInfos.add(infoDto);
        }

        return productInfos;
    }
}
