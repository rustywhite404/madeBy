package com.madeby.orderservice.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.client.CartServiceClient;
import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.dto.*;
import com.madeby.orderservice.entity.*;
import com.madeby.orderservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductSnapshotRepository snapshotRepository;
    private final CartServiceClient cartServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public void requestReturn(Long orderId, Long userId) {
        // 1. 주문 조회
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));

        // 2. 주문 유저 확인
        if (!order.getUserId().equals(userId)) {
            throw new MadeByException(MadeByErrorCode.NOT_YOUR_ORDER);
        }

        // 3. 반품 가능 여부 확인: 배송 완료 상태인지 확인
        if (!order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new MadeByException(MadeByErrorCode.CANNOT_RETURN);
        }

        // 4. 반품 가능 여부 확인: 반품 가능한 주문인지 확인
        if (!order.isReturnable()) {
            throw new MadeByException(MadeByErrorCode.RETURN_NOT_ALLOWED);
        }

        // 4. 반품 요청 처리
        order.setStatus(OrderStatus.RETURN_REQUEST); // 상태를 반품 요청으로 변경
        order.setReturnRequestedDate(LocalDateTime.now()); // 반품 요청 날짜 설정
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        // 1. 주문 조회
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));

        // 2. 주문 유저 확인
        if (!order.getUserId().equals(userId)) {
            throw new MadeByException(MadeByErrorCode.NOT_YOUR_ORDER);
        }

        // 3. 주문 상태 확인
        if (!order.getStatus().equals(OrderStatus.ORDERED)) {
            throw new MadeByException(MadeByErrorCode.CANNOT_CANCEL_ORDER);
        }

        // 4. 주문 상품 스냅샷을 통해 재고 복구
        List<OrderProductSnapshot> snapshots = order.getOrderProductSnapshots();
        for (OrderProductSnapshot snapshot : snapshots) {
            ProductInfoDto productInfo = productServiceClient.getProductInfo(snapshot.getProductInfoId());
            if (productInfo == null) {
                throw new MadeByException(MadeByErrorCode.NO_PRODUCT);
            }
            // 재고 복구
            productInfo.setStock(productInfo.getStock() + snapshot.getQuantity());
        }

        // 5. 주문 상태 변경
        order.setStatus(OrderStatus.CANCELED);
    }

    //주문 생성(장바구니에서 여러 상품 주문)
    @Transactional
    public Long placeOrderFromCart(Long userId, List<OrderRequestDto> orderRequestDtos) {

        // 1. 유저 검증 로직 제거 (API Gateway가 이미 검증함)

        // 2. 유저의 장바구니 확인
        CartResponseDto cart = cartServiceClient.getCartByUserId(userId);

        // 3. 주문 생성
        Orders order = Orders.builder()
                .userId(userId) // userId 설정
                .status(OrderStatus.ORDERED)
                .isReturnable(true)
                .orderProductSnapshots(new ArrayList<>()) // 초기화
                .build();
        orderRepository.save(order);

        // 4. 장바구니 내 주문 검증 및 총 주문 금액 계산
        BigDecimal totalAmount = BigDecimal.ZERO; // 주문 총 금액
        List<OrderProductSnapshot> snapshots = new ArrayList<>(); // 주문 스냅샷 리스트

        for (OrderRequestDto orderRequest : orderRequestDtos) {
            Long productInfoId = orderRequest.getProductInfoId();
            int quantity = orderRequest.getQuantity();

            // 4-2. 상품 정보 확인
            ProductInfoDto productInfoDto = productServiceClient.getProductInfo(productInfoId);
            if (productInfoDto == null) {
                throw new MadeByException(MadeByErrorCode.NO_PRODUCT, "상품 정보를 가져올 수 없습니다: " + productInfoId);
            }

            ProductsDto productsDto = productServiceClient.getProduct(productInfoDto.getId());
            if (productsDto == null || !productsDto.isVisible()) {
                throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT, "해당 상품은 판매 중이 아닙니다: " + productInfoDto.getId());
            }

            if (!productsDto.isVisible()) {
                throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT);
            }

            // 5. 품절 및 재고 확인
            if (productInfoDto.getStock() <= 0) {
                throw new MadeByException(MadeByErrorCode.SOLD_OUT);
            }

            if (productInfoDto.getStock() < quantity) {
                throw new MadeByException(MadeByErrorCode.DECREMENT_STOCK_FAILURE);
            }

            // 6. 재고 감소 (동시성 처리)
            try {
                productServiceClient.decrementStock(productInfoId, quantity);
            } catch (Exception e) {
                throw new MadeByException(MadeByErrorCode.DECREMENT_STOCK_FAILURE, "재고 감소 처리 중 오류가 발생했습니다: " + productInfoId);
            }

            // 7. 주문 상품 스냅샷 생성
            OrderProductSnapshot snapshot = OrderProductSnapshot.builder()
                    .orders(order)
                    .productInfoId(productInfoDto.getId())
                    .productsName(productsDto.getName()) // ProductsDto에서 상품명 가져오기
                    .stock(productInfoDto.getStock()) // ProductInfoDto에서 재고 가져오기
                    .size(productInfoDto.getSize()) // ProductsDto에서 사이즈 가져오기
                    .color(productInfoDto.getColor()) // ProductsDto에서 색상 가져오기
                    .quantity(quantity) // 요청된 수량
                    .price(productInfoDto.getPrice()) // ProductInfoDto에서 가격 가져오기
                    .totalAmount(productInfoDto.getPrice().multiply(BigDecimal.valueOf(quantity))) // 총 금액 계산
                    .build();


            snapshots.add(snapshot);

            // 총 금액 계산
            totalAmount = totalAmount.add(snapshot.getTotalAmount());
        }

        // 8. 주문 상품 스냅샷 저장
        for (OrderProductSnapshot snapshot : snapshots) {
            snapshot.setOrders(order);
            order.getOrderProductSnapshots().add(snapshot); // 주문 객체에 스냅샷 추가
        }

        // 9. 장바구니에서 주문 완료된 상품 제거
        for (OrderRequestDto orderRequest : orderRequestDtos) {
            cartServiceClient.removeProductFromCart(userId, orderRequest.getProductInfoId());
        }

        // 6. 장바구니 비우기 (Cart-Service 호출)
        cartServiceClient.clearCart(userId);

        return order.getId();
    }


    @Transactional(readOnly = true)
    public Orders findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));
    }

    // 주문 생성(단일 상품 주문)
    @Transactional
    public Long placeOrder(Long userId, Long productInfoId, int quantity) {
        // 1. 상품 정보 가져오기 (Feign Client 사용)
        ProductInfoDto productInfoDto = productServiceClient.getProductInfo(productInfoId);
        if (productInfoDto == null) {
            throw new MadeByException(MadeByErrorCode.NO_PRODUCT);
        }

        ProductsDto product = productServiceClient.getProduct(productInfoDto.getProductId());
        if (!product.isVisible()) {
            throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT);
        }

        // 3. 재고 확인
        if (productInfoDto.getStock() < quantity) {
            throw new MadeByException(MadeByErrorCode.SOLD_OUT);
        }

        // 4. 재고 감소 (Feign Client 사용)
        try {
            productServiceClient.decrementStock(productInfoId, quantity);
        } catch (Exception e) {
            throw new MadeByException(MadeByErrorCode.DECREMENT_STOCK_FAILURE, "재고 감소 처리 중 오류가 발생했습니다.");
        }

        // 5. 주문 생성 및 저장
        Orders order = Orders.builder()
                .userId(userId)
                .status(OrderStatus.ORDERED)
                .isReturnable(true) // 기본값으로 설정
                .build();
        orderRepository.save(order);

        // 6. 주문 상품 스냅샷 생성
        OrderProductSnapshot snapshot = OrderProductSnapshot.builder()
                .orders(order)
                .productInfoId(productInfoDto.getId())
                .productsName(product.getName()) // ProductsDto에서 상품명 가져오기
                .stock(productInfoDto.getStock()) // ProductInfoDto에서 재고 가져오기
                .size(productInfoDto.getSize()) // ProductsDto에서 사이즈 가져오기
                .color(productInfoDto.getColor()) // ProductsDto에서 색상 가져오기
                .quantity(quantity) // 요청된 수량
                .price(productInfoDto.getPrice()) // ProductInfoDto에서 가격 가져오기
                .totalAmount(productInfoDto.getPrice().multiply(BigDecimal.valueOf(quantity))) // 총 금액 계산
                .build();
        snapshotRepository.save(snapshot);

        return order.getId();
    }

    // 주문 조회(조회만 수행하므로 읽기 전용 트랜잭션)
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders(Long userId, LocalDate startDate, LocalDate endDate, Long cursor, int size) {
        // 조회 날짜 범위 기본값 설정(3개월 이내)
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(3);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        // LocalDate → LocalDateTime 변환
        LocalDateTime startDateTime = startDate.atStartOfDay(); // 00:00:00
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX); // 23:59:59.999999999

        // 주문 내역 조회
        List<Orders> orders = orderRepository.findOrdersByUserIdAndDateWithCursor(
                userId, startDateTime, endDateTime, cursor, size
        );

        // 결과가 비어 있으면 예외 발생
        if (orders.isEmpty()) {
            throw new MadeByException(MadeByErrorCode.NO_ORDER);
        }

        // DTO 변환
        return orders.stream()
                .map(OrderResponseDto::fromEntity)
                .toList();
    }
}