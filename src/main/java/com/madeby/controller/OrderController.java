package com.madeby.controller;

import com.madeby.common.ApiResponse;
import com.madeby.dto.OrderRequestDto;
import com.madeby.exception.MadeByErrorCode;
import com.madeby.exception.MadeByException;
import com.madeby.security.UserDetailsImpl;
import com.madeby.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}