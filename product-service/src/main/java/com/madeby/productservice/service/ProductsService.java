package com.madeby.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.productservice.dto.ProductInfoDto;
import com.madeby.productservice.dto.ProductsDto;
import com.madeby.productservice.dto.ProductsWithoutInfoDto;
import com.madeby.productservice.entity.ProductDocument;
import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.ProductInfoDocument;
import com.madeby.productservice.entity.Products;
import com.madeby.productservice.elasticsearch.ProductElasticsearchRepository;
import com.madeby.productservice.repository.ProductInfoRepository;
import com.madeby.productservice.repository.ProductsRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductsService {

    private static final int CACHE_PAGE_LIMIT = 3;  // 캐시할 최대 페이지 수

    private final ProductsRepository productsRepository;
    private final ProductInfoRepository productInfoRepository;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final RedisTemplate redisTemplate;
    private final ProductElasticsearchRepository productElasticsearchRepository;

    @Transactional
    public ProductInfoDto createLimitedProductInfo(Long productId, ProductInfoDto productInfoDto) throws JsonProcessingException {
        // 1. 상품 존재 여부 확인
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // 2. 기존 옵션과 중복 여부 확인
        boolean isDuplicateOption = productInfoRepository.existsByProductsAndColorAndSize(
                product, productInfoDto.getColor(), productInfoDto.getSize()
        );

        if (isDuplicateOption) {
            throw new MadeByException(MadeByErrorCode.DUPLICATE_OPTION,MadeByErrorCode.DUPLICATE_OPTION.getMessage());
        }

        // 3. 새로운 옵션 생성
        ProductInfo productInfo = ProductInfo.builder()
                .products(product)
                .price(productInfoDto.getPrice())
                .stock(productInfoDto.getStock())
                .size(productInfoDto.getSize())
                .color(productInfoDto.getColor())
                .isLimited(true)  // 항상 true로 설정
                .isVisible(false)  // 기본적으로 false로 설정
                .build();

        // 4. 저장
        productInfo = productInfoRepository.save(productInfo);

        // 5. Redis에 저장
        ProductInfoDto savedDto = ProductInfoDto.fromEntity(productInfo);
        String redisKey = "product_info:" + productInfo.getId();
        redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(productInfoDto));


        return savedDto;
    }


    @Transactional
    public void updateLimitedProductsVisibility() {
        List<ProductInfo> limitedProducts = productInfoRepository.findByIsLimitedTrue();
        for (ProductInfo productInfo : limitedProducts) {
            // DB 업데이트
            productInfo.setVisible(true);
            productInfoRepository.save(productInfo);

            // Redis 업데이트
            String redisKey = "product_info:" + productInfo.getId();
            try {
                // 기존 Redis 데이터 가져오기
                String jsonStr = (String) redisTemplate.opsForValue().get(redisKey);
                if (jsonStr != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> productMap = objectMapper.readValue(jsonStr, Map.class);

                    // isVisible 값을 true로 업데이트
                    productMap.put("isVisible", true);
                    productMap.put("visible", true);

                    // 수정된 데이터를 다시 Redis에 저장
                    redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(productMap));
                    log.info("Redis 상품 정보 업데이트 성공: productInfoId = {}", productInfo.getId());
                }
            } catch (Exception e) {
                log.error("Redis 상품 정보 업데이트 실패: productInfoId = {}, error = {}",
                        productInfo.getId(), e.getMessage());
            }
        }
        log.info("한정판 상품 업데이트 완료: 총 {}개 상품", limitedProducts.size());
    }

    @Transactional
    public Products registerNewProduct(ProductsDto productsDto) {
        // 1. Products 엔티티 생성
        Products product = Products.builder()
                .name(productsDto.getName())
                .category(productsDto.getCategory())
                .image(productsDto.getImage())
                .description(productsDto.getDescription())
                .build();

        // 2. 연관된 ProductInfo 엔티티 생성
        for (ProductInfoDto infoDto : productsDto.getProductInfos()) {
            ProductInfo info = ProductInfo.builder()
                    .products(product)
                    .price(infoDto.getPrice())
                    .stock(infoDto.getStock())
                    .size(infoDto.getSize())
                    .color(infoDto.getColor())
                    .isLimited(infoDto.isLimited())
                    .isVisible(infoDto.isVisible())
                    .build();

            // 3. isLimited가 true일 경우, Products의 isVisible을 false로 설정
            if (info.isLimited()) {
                info.setVisible(false); // 한정판매 제품이면 Products의 isVisible을 false로 설정
            }

            product.getProductInfos().add(info);

            // 4. Redis에 초기 재고 등록
            String redisKey = "product_stock:" + info.getId();
            redissonClient.getBucket(redisKey).set(info.getStock());
        }
        Products savedProduct = productsRepository.save(product);

        // Elasticsearch용 문서로 변환 후 저장
        ProductDocument productDocument = convertToDocument(savedProduct);
        ProductDocument savedDocument = productElasticsearchRepository.save(productDocument);

        return savedProduct;
    }

    private ProductDocument convertToDocument(Products product) {
        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .image(product.getImage())
                .isVisible(true)
                .productInfos(product.getProductInfos().stream()
                        .map(info -> ProductInfoDocument.builder()
                                .id(info.getId())
                                .price(info.getPrice())
                                .stock(info.getStock())
                                .size(info.getSize())
                                .color(info.getColor())
                                .isLimited(info.isLimited())
                                .isVisible(info.isVisible())
                                .build())
                        .toList())
                .build();
    }

    @Cacheable(
            value = "products",
            key = "'cursor:' + (#cursor ?: 'default') + ':size:' + #size",
            unless = "#result.content.isEmpty() || T(java.lang.Math).floor((#cursor ?: 10) / #size) > 10"
    )
    @Transactional(readOnly = true)
    public Slice<ProductsWithoutInfoDto> getProducts(Long cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));

        if (cursor == null) {
            cursor = 10L; // 기본값 설정
        }
        log.info("Fetching products with cursor: {}, size: {}", cursor, size);
        List<ProductsWithoutInfoDto> products = productsRepository.findByIdLessThanWithoutProductInfos(cursor, pageRequest);

        Long nextCursor = products.isEmpty() ? null : products.get(products.size() - 1).getId();

        return new SliceImpl<>(products, pageRequest, nextCursor != null);
    }


    @Transactional(readOnly = true)
    public ProductsDto getProductWithInfos(Long productId) {
        // Products와 연관된 ProductInfo를 함께 조회
        Products product = productsRepository.findByIdAndIsVisibleTrue(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // ProductInfo 중 isVisible=true인 옵션만 포함
        List<ProductInfoDto> visibleInfos = product.getProductInfos().stream()
                .filter(ProductInfo::isVisible) // isVisible=true 필터링
                .map(ProductInfoDto::fromEntity) // Dto 변환
                .toList();

        // ProductsDto 생성 및 반환
        ProductsDto productDto = convertToProductsDto(product);
        productDto.setProductInfos(visibleInfos);

        return productDto;
    }

    // Products -> ProductsDto 변환 로직
    private ProductsDto convertToProductsDto(Products product) {
        return ProductsDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .image(product.getImage())
                .category(product.getCategory())
                .productInfos(product.getProductInfos().stream()
                        .filter(ProductInfo::isVisible)
                        .map(info -> ProductInfoDto.builder()
                                .id(info.getId())
                                .price(info.getPrice())
                                .stock(info.getStock())
                                .size(info.getSize())
                                .color(info.getColor())
                                .isLimited(info.isLimited()) // 한정 여부
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public ProductInfoDto getProductInfo(Long productInfoId) {

        ProductInfo productInfo = productInfoRepository.findById(productInfoId).orElseThrow(
                () -> new MadeByException(MadeByErrorCode.NO_PRODUCT)
        );
        return ProductInfoDto.fromEntity(productInfo);
    }

    public ProductsDto getProduct(Long productId) {
        // 1. Product 조회
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // 2. ProductsDto로 변환하여 반환
        return ProductsDto.builder()
                .id(product.getId())
                .name(product.getName())
                .image(product.getImage())
                .description(product.getDescription())
                .category(product.getCategory())
                .build();
    }

    @Transactional
    public boolean updateStock(Long productInfoId, int quantity) {
        ProductInfo productInfo = productInfoRepository.findById(productInfoId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // 재고 업데이트 로직
        int newStock = quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        productInfo.setStock(newStock);

        // Redis에 동기화
        String redisKey = "product_stock:" + productInfoId;
        redissonClient.getBucket(redisKey).set(newStock);
        return true;
    }

    @Transactional
    public boolean decrementStock(Long productInfoId, int quantity) {
        try {
            // 데이터베이스에서 재고 감소
            int updatedRows = productInfoRepository.decrementStock(productInfoId, quantity);
            if (updatedRows == 0) {
                log.warn("재고 부족: productInfoId = {}, 요청 수량 = {}", productInfoId, quantity);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("재고 감소 실패: productInfoId = {}, 오류 = {}", productInfoId, e.getMessage());
            return false;
        }
    }

    @Cacheable(value = "productSearch",
            key = "'search:' + (#name?.trim() ?: '') + ':' + (#cursor ?: 0) + ':' + #size",
            unless = "#result.getContent().isEmpty()")
    @Transactional(readOnly = true)
    public Slice<ProductsWithoutInfoDto> searchProductsByName(String name, Long cursor, int size) {
        Pageable pageable = PageRequest.of(0, size); // 한 페이지 크기 지정

        try {
            log.info("Elasticsearch query: name={}, from={}, size={}", name, cursor, size);
            // 1. Elasticsearch에서 먼저 검색
            Page<ProductDocument> searchResults = productElasticsearchRepository
                    .findByNameContainingIgnoreCase(name.trim(), pageable);

            if (!searchResults.isEmpty()) {
                // Elasticsearch 결과를 ProductsWithoutInfoDto로 변환
                List<ProductsWithoutInfoDto> productsInfo = searchResults.getContent().stream()
                        .map(product -> new ProductsWithoutInfoDto(
                                product.getId(),
                                product.getName(),
                                product.getImage(),
                                product.getDescription(),
                                product.getCategory()
                        ))
                        .toList();

                return new SliceImpl<>(productsInfo, pageable, searchResults.hasNext());
            }
        } catch (Exception e) {
            log.error("Elasticsearch search failed: ", e);  // 에러 로그 추가
        }

        // 2. Elasticsearch에서 결과가 없거나 에러 발생 시 Caffeine 캐시 사용, 캐시에도 없을 경우 DB 검색으로 폴백
        log.info("No results found in Elasticsearch, falling back to database search");
        List<ProductsWithoutInfoDto> products = productsRepository
                .searchByNameWithCursor(name.trim(), cursor, pageable);

        Long nextCursor = products.isEmpty() ? null : products.get(products.size() - 1).getId();

        return new SliceImpl<>(products, pageable, nextCursor != null);
    }

    // 전체 캐시 내용 확인
    public Map<Object, Object> getAllCacheContents(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            log.error("Cache '{}' not found!", cacheName);
            throw new MadeByException(MadeByErrorCode.NO_CACHE);
        }

        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
            // Caffeine Cache로 캐스팅
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCache;
            Map<Object, Object> cacheContents = caffeineCache.asMap();
            log.debug("Cache contents: {}", cacheContents);
            return cacheContents;
        } else {
            log.error("Cache '{}' is not a Caffeine Cache instance!", cacheName);
            throw new MadeByException(MadeByErrorCode.NO_CACHE);
        }
    }

    @Transactional
    @CachePut(value = "products", key = "#productId")
    public ProductsDto updateProduct(Long productId, ProductsDto productsDto) {
        // DB에서 상품 조회
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // 상품 정보 업데이트
        product.setName(productsDto.getName());
        product.setCategory(productsDto.getCategory());
        product.setImage(productsDto.getImage());
        product.setDescription(productsDto.getDescription());

        // 저장 및 캐시 갱신
        Products updatedProduct = productsRepository.save(product);

        // Elasticsearch에 데이터 동기화
        try {
            ProductDocument productDocument = convertToProductDocument(updatedProduct); // DTO → Elasticsearch 문서 변환
            productElasticsearchRepository.save(productDocument);
            log.info("Elasticsearch 동기화 성공: {}", productDocument);
        } catch (Exception e) {
            log.error("Elasticsearch 동기화 실패", e);
            // 실패 처리 로직 (예: 별도 큐에 저장하여 재처리)
        }

        // DTO로 변환하여 반환
        return convertToProductsDto(updatedProduct);
    }

    private ProductDocument convertToProductDocument(Products product) {
        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .image(product.getImage())
                .description(product.getDescription())
                .isVisible(product.isVisible())
                .build();
    }

    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public void deleteProduct(Long productId) {
        // DB에서 상품 조회
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // DB, Elastic Search에서 삭제
        productsRepository.delete(product);
        productElasticsearchRepository.deleteById(productId);
    }

}
