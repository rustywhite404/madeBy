package com.madeby.service;

import com.madeby.dto.ProductInfoResponseDto;
import com.madeby.dto.ProductResponseDto;
import com.madeby.entity.Products;
import com.madeby.exception.MadeByErrorCode;
import com.madeby.exception.MadeByException;
import com.madeby.repository.ProductsRepository;
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
    public Slice<ProductResponseDto> getProducts(Long cursor, int size) {
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

        return productSlice.map(product -> ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .image(product.getImage())
                .description(product.getDescription())
                .category(product.getCategory())
                .build());
    }

    public ProductResponseDto getProductWithInfos(Long productId) {
        // Products와 연관된 ProductInfo를 함께 조회
        Products product = productsRepository.findByIdAndIsVisibleTrue(productId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        // ProductResponseDto로 변환
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .image(product.getImage())
                .category(product.getCategory())
                .registeredBy(product.getRegisteredBy())
                .infos(product.getProductInfos().stream()
                        .map(info -> ProductInfoResponseDto.builder()
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
