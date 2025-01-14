package com.madeby.payservice.controller;

import com.madeBy.shared.entity.PaymentStatus;
import com.madeby.payservice.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {
    private final PayService payService;

    @PostMapping("/process-payment")
    public PaymentStatus processPayment(@RequestParam Long orderId,
                                        @RequestParam Long userId) {
        log.info("PayController.processPayment - orderId: {}, userId: {}", orderId, userId);
        PaymentStatus result = payService.processPayment(orderId, userId);
        return result;
    }

    @PostMapping("/initiate")
    public void initiatePayment(@RequestParam Long orderId,
                                @RequestParam Long userId) {
        log.info("[PayController] initiatePayment - orderId: {}, userId: {}", orderId, userId);
        payService.initiatePayment(orderId, userId);
    }

}
