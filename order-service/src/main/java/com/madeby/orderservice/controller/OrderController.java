package com.madeby.orderservice.controller;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.common.ApiResponse;
import com.madeby.orderservice.dto.OrderRequestDto;
import com.madeby.orderservice.dto.OrderResponseDto;
import com.madeby.orderservice.entity.Orders;
import com.madeby.orderservice.entity.User;
import com.madeby.orderservice.security.UserDetailsImpl;
import com.madeby.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/return")
    public ResponseEntity<Object> requestReturn(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User user = userDetails.getUser(); // 인증된 유저 정보
        orderService.requestReturn(orderId, user);

        return ResponseEntity.ok(ApiResponse.success("반품 신청이 정상적으로 접수 되었습니다."));
    }


    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<Object> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // 인증된 유저 정보
        User user = userDetails.getUser();

        // 주문 취소 서비스 호출
        orderService.cancelOrder(orderId, user);

        return ResponseEntity.ok(ApiResponse.success("주문이 정상적으로 취소 되었습니다."));
    }

    @PostMapping
    public
    ResponseEntity<ApiResponse<String>> placeOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody OrderRequestDto requestDto) {
        if (requestDto.getQuantity() <= 0) {
            throw new MadeByException(MadeByErrorCode.MIN_AMOUNT);
        }

        Long orderId = orderService.placeOrder(userDetails.getUser().getEmailHash(), requestDto.getProductInfoId(), requestDto.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("주문이 성공적으로 완료되었습니다. 주문 ID: " + orderId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 로그인 확인
        Long userId = userDetails.getUser().getId();

        // 주문 내역 조회
        List<OrderResponseDto> orders = orderService.getOrders(userId, startDate, endDate, cursor, size);

        return ResponseEntity.ok(orders);
    }

    @PostMapping("/cart")
    public ResponseEntity<OrderResponseDto> placeOrderFromCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody List<OrderRequestDto> orderRequestDtos) {

        // 인증된 유저 객체
        User user = userDetails.getUser();

        // 서비스 호출
        Long orderId = orderService.placeOrderFromCart(user, orderRequestDtos);

        // 주문 응답 생성
        Orders order = orderService.findOrderById(orderId);
        OrderResponseDto response = OrderResponseDto.fromEntity(order);

        return ResponseEntity.ok(response);
    }

}