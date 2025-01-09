package com.madeby.orderservice.service;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.client.CartServiceClient;
import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.dto.*;
import com.madeby.orderservice.entity.*;
import com.madeby.orderservice.repository.OrderProductSnapshotRepository;
import com.madeby.orderservice.repository.OrderRepository;
import com.madeby.orderservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final StockReservationService stockReservationService;
    private final ProductServiceClient productServiceClient;
    private final PaymentRepository paymentRepository;


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

            if (productInfoDto.getStock() < quantity) {
                throw new MadeByException(MadeByErrorCode.NOT_ENOUGH_PRODUCT);
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

        // 9. 결제 화면 진입
        initiatePayment(order.getId(), userId);

        // 5. 결제 시도 (모의 결제)
        PaymentStatus result = processPayment(order.getId(), userId);

        // 6. 결제 결과 처리
        if (result == PaymentStatus.COMPLETED) {
            log.info("결제 성공: 주문 ID = {}", order.getId());
            // 결제 성공한 경우 장바구니에서 주문 완료된 상품 제거
            for (OrderRequestDto orderRequest : orderRequestDtos) {
                cartServiceClient.removeProductFromCart(userId, orderRequest.getProductInfoId());
            }
        } else {
            log.info("결제 실패 또는 이탈: 주문 ID = {}", order.getId());
        }


        return order.getId();
    }


    @Transactional(readOnly = true)
    public Orders findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));
    }

    // 주문 생성(단일 상품 주문)
    @Transactional
    public PaymentStatus placeOrder(Long userId, Long productInfoId, int quantity) {

        // 1. 상품 정보 가져오기 (Feign Client 사용)
        ProductInfoDto productInfoDto = productServiceClient.getProductInfo(productInfoId);
        if (productInfoDto == null) {
            throw new MadeByException(MadeByErrorCode.NO_PRODUCT);
        }

        // 2. 판매중인 상품인지 확인
        if (!productInfoDto.isVisible()) {
            throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT);
        }

        // 3. 재고 확인 및 예약
        if (!stockReservationService.reserveStock(productInfoId, quantity)) {
            throw new MadeByException(MadeByErrorCode.NOT_ENOUGH_PRODUCT);
        }
        log.info("재고 예약 성공: productInfoId = {}, quantity = {}", productInfoId, quantity);

        // 4. 주문 생성 및 저장
        Orders order = Orders.builder()
                .userId(userId)
                .status(OrderStatus.ORDERED)
                .isReturnable(true) // 기본값으로 설정
                .build();
        orderRepository.save(order);

        // 4-1. 주문 상품 스냅샷 생성
        OrderProductSnapshot snapshot = OrderProductSnapshot.builder()
                .orders(order)
                .productInfoId(productInfoDto.getId())
                .stock(productInfoDto.getStock()) // ProductInfoDto에서 재고 가져오기
                .size(productInfoDto.getSize()) // ProductsDto에서 사이즈 가져오기
                .color(productInfoDto.getColor()) // ProductsDto에서 색상 가져오기
                .quantity(quantity) // 요청된 수량
                .price(productInfoDto.getPrice()) // ProductInfoDto에서 가격 가져오기
                .totalAmount(productInfoDto.getPrice().multiply(BigDecimal.valueOf(quantity))) // 총 금액 계산
                .build();
        snapshotRepository.save(snapshot);

        // 5. 결제 화면 진입 처리
        initiatePayment(order.getId(), userId);

        // 6. 결제 시도 (모의 결제)
        PaymentStatus result = processPayment(order.getId(), userId);

        // 7. 결제 결과 처리
        if (result == PaymentStatus.COMPLETED) {
            log.info("결제 성공: 주문 ID = {}", order.getId());
            // DB에 최종 재고 반영
            boolean decrementSuccess = productServiceClient.decrementStock(productInfoId, quantity);
            if (!decrementSuccess) {
                stockReservationService.cancelReservation(productInfoId, quantity);
                throw new MadeByException(MadeByErrorCode.NOT_ENOUGH_PRODUCT, "재고가 부족합니다");
            }
        } else {
            log.info("결제 실패 또는 이탈: 주문 ID = {}", order.getId());
            // Redis에 재고 복구
            stockReservationService.cancelReservation(productInfoId, quantity);
        }
        return result;
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

    @Transactional
    public void initiatePayment(Long orderId, Long userId) {
        // 1. 주문 조회
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));

        // 2. 유저 확인
        if (!order.getUserId().equals(userId)) {
            throw new MadeByException(MadeByErrorCode.NOT_YOUR_ORDER);
        }

        // 3. 결제 상태 확인
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> Payment.builder()
                        .orderId(orderId)
                        .status(PaymentStatus.PENDING) // '결제시도' 상태
                        .build());

        paymentRepository.save(payment);

        log.info("결제 화면으로 진입: 주문 ID = {}, 결제 상태 = {}", orderId, payment.getStatus());
    }

    @Transactional
    public PaymentStatus processPayment(Long orderId, Long userId) {
        // 1. 결제 데이터 조회
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_PAYMENT));

         //2. 고객 이탈율 시뮬레이션 (20% 확률로 결제 시도 중단)
        if (Math.random() < 0.2) {
            log.info("고객 이탈: 결제 시도 중단 (주문 ID: {})", orderId);
            payment.setStatus(PaymentStatus.CANCELED);
            return PaymentStatus.CANCELED;
        }

        // 3. 결제 상태 변경 ('결제중')
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // 4. 결제 완료 시뮬레이션 (20% 확률로 결제 실패)
        if (Math.random() < 0.2) {
            log.info("결제 실패: 고객 사유 (주문 ID: {})", orderId);
            payment.setStatus(PaymentStatus.FAILED);
            return PaymentStatus.FAILED;
        }

        // 5. 결제 성공
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("결제 완료: 주문 ID = {}, 결제 상태 = {}", orderId, payment.getStatus());
        return PaymentStatus.COMPLETED;
    }

}