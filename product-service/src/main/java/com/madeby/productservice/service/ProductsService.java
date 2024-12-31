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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductsService {

    private final ProductsRepository productsRepository;
    private final ProductInfoRepository productInfoRepository;

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

        // ProductsDto로 변환
        return convertToProductsDto(product);
    }

    // Products -> ProductsDto 변환 로직 추출
    private ProductsDto convertToProductsDto(Products product) {
        return ProductsDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .image(product.getImage())
                .category(product.getCategory())
                .productInfos(product.getProductInfos().stream()
                        .map(info -> ProductInfoDto.builder()
                                .id(info.getId())
                                .price(info.getPrice())
                                .stock(info.getStock())
                                .size(info.getSize())
                                .color(info.getColor())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public ProductInfoDto getProductInfo(Long productInfoId) {
        ProductInfo productInfo = productInfoRepository.findById(productInfoId).orElseThrow(
                ()-> new MadeByException(MadeByErrorCode.NO_PRODUCT)
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
                .isVisible(product.isVisible())
                .build();
    }

    @Transactional
    public boolean updateStock(Long productInfoId, int quantity) {
        ProductInfo productInfo = productInfoRepository.findById(productInfoId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // 재고 업데이트 로직
        productInfo.setStock(productInfo.getStock() + quantity);
        if (productInfo.getStock() < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        return true;
    }

    public void decrementStock(Long productInfoId, int quantity) {
        // productInfoId로 상품 정보 조회
        ProductInfo productInfo = productInfoRepository.findById(productInfoId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        log.info("[decrementStock] 재고 감소 처리 시작 - productInfoId: {}, 현재 재고: {}, 감소 수량: {}",
                productInfoId, productInfo.getStock(), quantity);

        // 재고 확인
        if (productInfo.getStock() < quantity) {
            throw new MadeByException(MadeByErrorCode.SOLD_OUT, "재고가 부족합니다.");
        }

        // 재고 감소
        productInfo.setStock(productInfo.getStock() - quantity);
        productInfoRepository.save(productInfo);

        log.info("[decrementStock] 재고 감소 처리 완료 - productInfoId: {}, 남은 재고: {}",
                productInfoId, productInfo.getStock());
    }
}
