package com.madeby.userservice.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.userservice.dto.ProductInfoDto;
import com.madeby.userservice.dto.ProductsDto;
import com.madeby.userservice.entity.Products;
import com.madeby.userservice.repository.ProductsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductsService {

    private final ProductsRepository productsRepository;

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
}
