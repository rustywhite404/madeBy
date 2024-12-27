package com.madeby.orderservice.controller;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.common.ApiResponse;
import com.madeby.orderservice.dto.ProductsDto;
import com.madeby.orderservice.service.ProductsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {
    private final ProductsService productsService;

    //상품 목록 보기
    @GetMapping("/products")
    public  ResponseEntity<ApiResponse<List<ProductsDto>>> getProducts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        if (size <= 0 || size > 100) {
            throw new MadeByException(MadeByErrorCode.OUT_OF_RANGE);
        }

        List<ProductsDto> products = productsService.getProducts(cursor, size).getContent();

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    //상품 상세 보기
    @GetMapping("/products/{productId}")
    public
    ResponseEntity<ApiResponse<ProductsDto>> getProductDetail(@PathVariable Long productId) {
        ProductsDto productDetail = productsService.getProductWithInfos(productId);
        return ResponseEntity.ok(ApiResponse.success(productDetail));
    }
}
