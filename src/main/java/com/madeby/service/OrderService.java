package com.madeby.service;

import com.madeby.dto.OrderResponseDto;
import com.madeby.entity.*;
import com.madeby.exception.MadeByErrorCode;
import com.madeby.exception.MadeByException;
import com.madeby.repository.OrderProductSnapshotRepository;
import com.madeby.repository.OrderRepository;
import com.madeby.repository.ProductInfoRepository;
import com.madeby.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductInfoRepository productInfoRepository;
    private final OrderRepository orderRepository;
    private final OrderProductSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;

    // 주문 생성
    @Transactional
    public Long placeOrder(String userEmailHash, Long productInfoId, int quantity) {
        // 1. 유저 확인
        User user = userRepository.findByEmailHash(userEmailHash)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.USER_NOT_LOGIN));

        // 2. 상품 및 옵션 확인
        ProductInfo productInfo = productInfoRepository.findById(productInfoId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PRODUCT));

        Products product = productInfo.getProducts();
        if (!product.isVisible()) {
            throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT);
        }

        // 3. 재고 확인
        if (productInfo.getStock() < quantity) {
            throw new MadeByException(MadeByErrorCode.SOLD_OUT);
        }

        // 4. 재고 감소 (동시성 처리)
        int stockUpdated = productInfoRepository.decrementStock(productInfoId, quantity);
        if (stockUpdated<=0) {
            throw new MadeByException(MadeByErrorCode.DECREMENT_STOCK_FAILURE);
        }

        // 5. 주문 생성 및 저장
        Orders order = Orders.builder()
                .user(user)
                .status(OrderStatus.ORDERED)
                .isReturnable(true) // 기본값으로 설정
                .build();
        orderRepository.save(order);

        // 6. 주문 상품 스냅샷 생성
        OrderProductSnapshot snapshot = OrderProductSnapshot.builder()
                .orders(order)
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImage())
                .productDescription(product.getDescription())
                .category(product.getCategory())
                .quantity(quantity)
                .price(BigDecimal.valueOf(productInfo.getPrice()))
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

        // createdAt이 LocalDateTime이므로 LocalDate → LocalDateTime 변환
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

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