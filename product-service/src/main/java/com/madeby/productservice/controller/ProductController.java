package com.madeby.productservice.controller;

import com.madeBy.shared.common.ApiResponse;
import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.productservice.dto.ProductInfoDto;
import com.madeby.productservice.dto.ProductsDto;
import com.madeby.productservice.dto.ProductsWithoutInfoDto;
import com.madeby.productservice.entity.Products;
import com.madeby.productservice.service.ProductsService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductsService productsService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Products>> registerProduct(@RequestBody ProductsDto productsDto) {
        Products registeredProduct = productsService.registerNewProduct(productsDto);
        return ResponseEntity.ok(ApiResponse.success(registeredProduct));
    }

    @PostMapping("/products/{productId}/limitedOptionRegister")
    public ResponseEntity<ApiResponse<ProductInfoDto>> createProductInfo(
            @PathVariable Long productId,
            @RequestBody ProductInfoDto productInfoDto) {
        ProductInfoDto createdInfo = productsService.createLimitedProductInfo(productId, productInfoDto);
        return ResponseEntity.ok(ApiResponse.success(createdInfo));
    }


    //재고 업데이트
    @PostMapping("/products/{productInfoId}/update-stock")
    public ResponseEntity<Boolean> updateStock(
            @PathVariable Long productInfoId,
            @RequestParam int quantity
    ) {
        boolean success = productsService.updateStock(productInfoId, quantity);
        return ResponseEntity.ok(success);
    }

    @PostMapping("/products/{productInfoId}/decrement-stock")
    public ResponseEntity<Boolean> decrementStock(
            @PathVariable Long productInfoId,
            @RequestParam int quantity) {
        log.info("[decrementStock] 요청 수신 - productInfoId: {}, quantity: {}", productInfoId, quantity);
        boolean result = productsService.decrementStock(productInfoId, quantity);
        return ResponseEntity.ok(result);
    }

    //상품 목록 보기
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductsWithoutInfoDto>>> getProducts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        if (size <= 0 || size > 100) {
            throw new MadeByException(MadeByErrorCode.OUT_OF_RANGE);
        }

        List<ProductsWithoutInfoDto> products = productsService.getProducts(cursor, size).getContent();

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // 상품 대분류 정보 가져오기
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductsDto> getProduct(@PathVariable Long productId) {
        ProductsDto product = productsService.getProduct(productId);
        return ResponseEntity.ok(product);
    }

    //상품 상세 한개만 가져오기(카트에 등록할 때 사용)
    @GetMapping("/products/info/{productInfoId}")
    public ProductInfoDto getProductInfo(@PathVariable Long productInfoId) {
        return productsService.getProductInfo(productInfoId);
    }

    //상품 상세 보기
    @GetMapping("/products/detail/{productId}")
    public ResponseEntity<ApiResponse<ProductsDto>> getProductDetail(@PathVariable Long productId) {
        ProductsDto productDetail = productsService.getProductWithInfos(productId);
        return ResponseEntity.ok(ApiResponse.success(productDetail));
    }

    //상품 검색
    @GetMapping("/products/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam String name,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") Integer size) {

        try {
            // cursor가 null일 경우 기본값 60000 설정(임시)
            Long adjustedCursor = cursor == null ? 60000L : cursor;

            Slice<ProductsWithoutInfoDto> results = productsService.searchProductsByName(name, cursor, size);

            return ResponseEntity.ok(new SliceResponse<>(
                    results.getContent(),
                    results.hasNext(),
                    results.getNumberOfElements()
            ));
        } catch (MadeByException e) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(MadeByErrorCode.NO_SEARCH_RESULT.name(), MadeByErrorCode.NO_SEARCH_RESULT.getMessage()));
        }
    }

    //캐시 조회(확인용)
    @GetMapping("/products/cache/all/{cacheName}")
    public ResponseEntity<?> getAllCacheContents(@PathVariable String cacheName) {
        try {
            Map<Object, Object> allCacheContents = productsService.getAllCacheContents(cacheName);
            return ResponseEntity.ok(allCacheContents);
        } catch (MadeByException e) {
            throw new MadeByException(MadeByErrorCode.NO_CACHE);
        }
    }


    // 슬라이스 응답을 위한 DTO
    @Getter
    @AllArgsConstructor
    class SliceResponse<T> {
        private List<T> content;
        private boolean hasNext;
        private int numberOfElements;
    }
}
