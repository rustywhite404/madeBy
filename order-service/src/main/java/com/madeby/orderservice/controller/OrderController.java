package com.madeby.orderservice.controller;

import com.madeBy.shared.common.ApiResponse;
import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.dto.OrderRequestDto;
import com.madeby.orderservice.dto.OrderResponseDto;
import com.madeby.orderservice.entity.Orders;
import com.madeby.orderservice.entity.PaymentStatus;
import com.madeby.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 결제 진입 API
    @PostMapping("/{orderId}/payment")
    public ResponseEntity<ApiResponse<String>> initiatePayment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId) {

        // 결제 진입 서비스 호출
        orderService.initiatePayment(orderId, userId);

        return ResponseEntity.ok(ApiResponse.success("결제 화면으로 진입하였습니다."));
    }

    // 결제 API
    @PostMapping("/{orderId}/payment/process")
    public ResponseEntity<ApiResponse<PaymentStatus>> processPayment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId) {

        // 결제 처리 서비스 호출
        PaymentStatus result = orderService.processPayment(orderId, userId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

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

        // 주문 취소 서비스 호출
        orderService.cancelOrder(orderId, userId);

        return ResponseEntity.ok(ApiResponse.success("주문이 정상적으로 취소 되었습니다."));
    }

    @PostMapping
    public
    ResponseEntity<ApiResponse<String>> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody OrderRequestDto requestDto) {
        if (requestDto.getQuantity() <= 0) {
            throw new MadeByException(MadeByErrorCode.MIN_AMOUNT);
        }

        Long orderId = orderService.placeOrder(userId, requestDto.getProductInfoId(), requestDto.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("주문이 성공적으로 완료되었습니다. 주문 ID: " + orderId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 주문 내역 조회
        List<OrderResponseDto> orders = orderService.getOrders(userId, startDate, endDate, cursor, size);

        return ResponseEntity.ok(orders);
    }

    @PostMapping("/cart")
    public ResponseEntity<OrderResponseDto> placeOrderFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<OrderRequestDto> orderRequestDtos) {

        // 서비스 호출
        Long orderId = orderService.placeOrderFromCart(userId, orderRequestDtos);

        // 주문 응답 생성
        Orders order = orderService.findOrderById(orderId);
        OrderResponseDto response = OrderResponseDto.fromEntity(order);

        return ResponseEntity.ok(response);
    }

}