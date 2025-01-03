package com.madeby.productservice.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.productservice.dto.ProductInfoDto;
import com.madeby.productservice.dto.ProductsDto;
import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.Products;
import com.madeby.productservice.repository.ProductInfoRepository;
import com.madeby.productservice.repository.ProductsRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductsService {

    private final ProductsRepository productsRepository;
    private final ProductInfoRepository productInfoRepository;
    private final RedissonClient redissonClient;

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

    @Transactional(readOnly = true)
    public Slice<ProductsDto> getProducts(Long cursor, int size) {
        // 최신 등록순으로(내림차순) 정렬
        PageRequest pageRequest = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));

        Slice<Products> productSlice;
        if (cursor == null) {
            // 커서가 없는 경우 최신 목록을 가져옴
            productSlice = productsRepository.findAllByIsVisibleTrue(pageRequest);
        } else {
            // 커서가 있는 경우, cursor를 기준으로 이전 데이터 가져옴
            productSlice = productsRepository.findByIsVisibleTrueAndIdLessThan(cursor, pageRequest);
        }

        // Products -> ProductsDto 변환
        return productSlice.map(this::convertToProductsDto);
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

    public void decrementStock(Long productInfoId, int quantity) {

        String redisKey = "product_stock:" + productInfoId;
        Integer currentStock = (Integer) redissonClient.getBucket(redisKey).get();

        if (currentStock == null || currentStock < quantity) {
            throw new MadeByException(MadeByErrorCode.NOT_ENOUGH_PRODUCT, "재고가 부족합니다.");
        }

        // Redis에서 재고 감소
        redissonClient.getBucket(redisKey).set(currentStock - quantity);


        // 데이터베이스에도 재고 감소
        ProductInfo productInfo = productInfoRepository.findById(productInfoId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));
        productInfo.setStock(currentStock - quantity);
        productInfoRepository.save(productInfo);

        log.info("[decrementStock] 재고 감소 처리 완료 - productInfoId: {}, 남은 재고: {}",
                productInfoId, currentStock - quantity);
    }
}
