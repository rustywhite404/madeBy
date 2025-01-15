package com.madeby.orderservice.controller;

import com.madeBy.shared.common.ApiResponse;
import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.dto.OrderProductSnapshotDto;
import com.madeby.orderservice.dto.OrderRequestDto;
import com.madeby.orderservice.dto.OrderResponseDto;
import com.madeby.orderservice.entity.Orders;
import com.madeBy.shared.entity.PaymentStatus;
import com.madeby.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RedisTemplate redisTemplate;

    @PostMapping("/{orderId}/return")
    public ResponseEntity<Object> requestReturn(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        orderService.requestReturn(orderId, userId); // 서비스로 ID만 전달하고, feignClient유틸에서 getUserById로 유저 정보를 조회하여 사용

        return ResponseEntity.ok(ApiResponse.success("반품 신청이 정상적으로 접수 되었습니다."));
    }

    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<Object> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        // 서비스 호출
        orderService.cancelOrder(orderId, userId);

        // 성공 응답 반환
        return ResponseEntity.ok(ApiResponse.success("주문이 정상적으로 취소되었습니다."));
    }

    @PostMapping
    public ResponseEntity<Object> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody OrderRequestDto requestDto) {

        // 유효성 검증
        if (requestDto.getQuantity() <= 0) {
            throw new MadeByException(MadeByErrorCode.MIN_AMOUNT, "주문 수량은 1개 이상이어야 합니다.");
        }

        Map<String, Object> response = orderService.placeOrder(userId, requestDto.getProductInfoId(), requestDto.getQuantity());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
    // 최종 상태를 확인하기 위한 Polling 메서드
    private PaymentStatus pollForFinalStatus(Long userId, Long productInfoId) {
        String redisKey = "order_status_" + userId + "_" + productInfoId;

        for (int i = 0; i < 10; i++) { // 최대 10번 시도
            Object statusObject = redisTemplate.opsForValue().get(redisKey);
            log.info("-----------[statusObject]:"+statusObject);
            if (statusObject instanceof String) { // Redis에서 문자열로 저장된 경우
                String status = (String) statusObject;
                try {
                    return PaymentStatus.valueOf(status); // Enum으로 변환
                } catch (IllegalArgumentException e) {
                    log.error("Redis에 저장된 상태가 올바르지 않습니다: {}", status, e);
                    throw new MadeByException(MadeByErrorCode.INVALID_STATUS, "주문 상태가 유효하지 않습니다.");
                }
            } else if (statusObject != null) {
                log.error("Redis에서 예상치 못한 데이터 타입을 반환했습니다: {}", statusObject.getClass());
                throw new MadeByException(MadeByErrorCode.INVALID_STATUS, "Redis에 저장된 상태 데이터가 유효하지 않습니다.");
            }

            try {
                Thread.sleep(500); // 500ms 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("상태 확인 중 인터럽트 발생", e);
            }
        }
        throw new MadeByException(MadeByErrorCode.STATUS_TIMEOUT, "주문 상태를 확인하지 못했습니다.");
    }


    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        // 주문 내역 조회
        List<OrderResponseDto> orders = orderService.getOrders(userId, startDate, endDate, cursor, size);

        return ResponseEntity.ok(orders);
    }

    @PostMapping("/cart")
    public ResponseEntity<Object> placeOrderFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<OrderRequestDto> orderRequestDtos) {

        Long orderId = orderService.placeOrderFromCart(userId, orderRequestDtos);
        Orders order = orderService.findOrderById(orderId);
        OrderResponseDto response = OrderResponseDto.fromEntity(order);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public OrderResponseDto getOrderDetails(@PathVariable Long orderId) {
        // 1. 주문 엔티티 조회
        Orders order = orderService.getOrderById(orderId);

        // 2. Orders → OrderResponseDto로 변환
        return convertToDto(order);
    }

    private OrderResponseDto convertToDto(Orders order) {
        // 스냅샷 목록 변환
        List<OrderProductSnapshotDto> snapshotDtos = order.getOrderProductSnapshots()
                .stream()
                .map(snapshot -> OrderProductSnapshotDto.builder()
                        .productInfoId(snapshot.getProductInfoId())
                        .stock(snapshot.getStock())
                        .size(snapshot.getSize())
                        .color(snapshot.getColor())
                        .quantity(snapshot.getQuantity())
                        .price(snapshot.getPrice())
                        .totalAmount(snapshot.getTotalAmount())
                        .build())
                .toList();

        // Orders → OrderResponseDto 매핑
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())  // OrderStatus enum일 경우 name() 등
                .totalAmount(snapshotDtos.stream()
                        .map(s -> s.getTotalAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)) // 예시: 스냅샷 총합
                .orderDate(order.getCreatedAt().toLocalDate())   // 예: createdAt이 있다면
                .isReturnable(order.isReturnable())
                .products(snapshotDtos)
                .build();
    }
}
