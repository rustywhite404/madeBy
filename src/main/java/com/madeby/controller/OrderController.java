package com.madeby.controller;

import com.madeby.common.ApiResponse;
import com.madeby.dto.OrderRequestDto;
import com.madeby.dto.OrderResponseDto;
import com.madeby.exception.MadeByErrorCode;
import com.madeby.exception.MadeByException;
import com.madeby.security.UserDetailsImpl;
import com.madeby.service.OrderService;
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

        log.info("startDate: {}, endDate: {}", startDate, endDate);
        // 로그인 확인
        Long userId = userDetails.getUser().getId();
        log.info("-------------로그인 한 userId:"+userId);

        // 주문 내역 조회
        List<OrderResponseDto> orders = orderService.getOrders(userId, startDate, endDate, cursor, size);

        return ResponseEntity.ok(orders);
    }

}