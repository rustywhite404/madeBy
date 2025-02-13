package com.madeby.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeBy.shared.entity.PaymentStatus;
import com.madeBy.shared.events.OrderCreatedEvent;
import com.madeBy.shared.events.OrderStatusUpdatedEvent;
import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.client.CartServiceClient;
import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.dto.*;
import com.madeby.orderservice.entity.*;
import com.madeby.orderservice.repository.OrderRepository;
import com.madeby.orderservice.client.PayServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor

public class OrderService {

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final StockReservationService stockReservationService;
    private final ProductServiceClient productServiceClient;
    private final PayServiceClient payServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Redis 키를 상수로 분리
    private static final String PRODUCT_INFO_REDIS_KEY_PREFIX = "product_info:";

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
        PaymentStatus result = payServiceClient.processPayment(order.getId(), userId);

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
    public Map<String, Object> placeOrder(Long userId, Long productInfoId, int quantity) {
        StringBuilder keyBuilder = new StringBuilder(PRODUCT_INFO_REDIS_KEY_PREFIX);
        keyBuilder.append(productInfoId);
        ProductInfoDto productInfoDto = null;
        Object cachedValue = redisTemplate.opsForValue().get(keyBuilder.toString());
        if (cachedValue instanceof String) {
            try {
                productInfoDto = objectMapper.readValue((String) cachedValue, ProductInfoDto.class);
            } catch (Exception e) {
                log.error("Redis 데이터 변환 실패: productInfoId={}", productInfoId);
            }
        }

        if (productInfoDto == null) {
            productInfoDto = productServiceClient.getProductInfo(productInfoId);
            if (productInfoDto == null) {
                throw new MadeByException(MadeByErrorCode.NO_PRODUCT);
            }
            log.info("상품 정보를 Feign Client에서 조회 성공: productInfoId = {}", productInfoId);
        }

        if (!productInfoDto.isVisible()) {
            throw new MadeByException(MadeByErrorCode.NO_SELLING_PRODUCT);
        }

        if (!stockReservationService.reserveStock(productInfoId, quantity)) {
            throw new MadeByException(MadeByErrorCode.NOT_ENOUGH_PRODUCT);
        }

        Orders order = new Orders(userId, OrderStatus.ORDERED, true);
        OrderProductSnapshot snapshot = new OrderProductSnapshot(
                order,
                productInfoId,
                productInfoDto.getStock(),
                productInfoDto.getSize(),
                productInfoDto.getColor(),
                quantity,
                productInfoDto.getPrice(),
                productInfoDto.getPrice().multiply(BigDecimal.valueOf(quantity))
        );

        order.getOrderProductSnapshots().add(snapshot);
        orderRepository.save(order);

        // 결제 시도 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                userId,
                productInfoId,
                quantity,
                "INITIATE_PAYMENT"
        );
        kafkaTemplate.send("order-created-topic", event);
        log.info("OrderCreatedEvent 발행: {}", event);

        // Kafka 리스너에서 결제 상태를 업데이트하기 전까지는 상태를 PENDING으로 반환
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        // 초기 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("orderStatus", "ORDERED");
        response.put("paymentStatus", paymentStatus.name());
        response.put("message", "주문이 성공적으로 생성되었습니다. 결제 상태를 확인 중입니다.");

        return response;
    }


    @KafkaListener(topics = "order-status-updated-topic", groupId = "order-service")
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("OrderStatusUpdatedEvent 수신: {}", event);

        Orders order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));

        if ("COMPLETED".equals(event.getStatus())) {
            log.info("결제 완료: 주문 ID = {}", order.getId());
        } else if ("FAILED".equals(event.getStatus())) {
            log.warn("결제 실패: 주문 ID = {}", order.getId());
            stockReservationService.cancelReservation(event.getProductInfoId(), event.getQuantity());
        }
        order.setStatus(OrderStatus.valueOf(event.getStatus()));
        orderRepository.save(order);
        // 최종 상태 Redis에 저장
        String redisKey = "order_status_" + order.getUserId() + "_" + event.getProductInfoId();
        redisTemplate.opsForValue().set(redisKey, event.getStatus(), Duration.ofMinutes(5)); // 5분 TTL
        log.info("주문 상태 Redis에 저장: key={}, status={}", redisKey, event.getStatus());
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

        // 3. 결제 상태 초기화
        payServiceClient.initiatePayment(orderId, userId);
    }


    @Transactional(readOnly = true)
    public Orders getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new MadeByException(MadeByErrorCode.NO_ORDER));
    }
}