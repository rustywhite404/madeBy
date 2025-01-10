package com.madeby.productservice.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.productservice.dto.ProductInfoDto;
import com.madeby.productservice.dto.ProductsDto;
import com.madeby.productservice.dto.ProductsWithoutInfoDto;
import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.Products;
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
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
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

    @Transactional
    public void updateLimitedProductsVisibility() {
        // 한정 상품들 중 isVisible=false인 상품들을 가져옴
        List<ProductInfo> limitedProducts = productInfoRepository.findByIsLimitedTrueAndIsVisibleFalse();

        if (limitedProducts.isEmpty()) {
            // 변경 대상이 없을 경우 로그만 출력하고 종료
            System.out.println("업데이트할 한정 상품이 없습니다.");
            return;
        }

        for (ProductInfo product : limitedProducts) {
            product.setVisible(true); // isVisible을 true로 변경
        }

        // 변경된 상품들 저장
        productInfoRepository.saveAll(limitedProducts);
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

            // Redis에 초기 재고 등록
            String redisKey = "product_stock:" + info.getId();
            redissonClient.getBucket(redisKey).set(info.getStock());
        }

        return productsRepository.save(product);
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

        // 연관 데이터 없이 ProductsWithoutInfoDto로 데이터 조회
        List<ProductsWithoutInfoDto> products = productsRepository.searchByNameWithCursor(name.trim(), cursor, pageable);

        // 다음 커서 계산
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

        // DTO로 변환하여 반환
        return convertToProductsDto(updatedProduct);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public void deleteProduct(Long productId) {
        // DB에서 상품 조회
        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // DB에서 삭제
        productsRepository.delete(product);
    }

}
