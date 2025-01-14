package com.madeby.orderservice.client;

import com.madeBy.shared.entity.PaymentStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "pay-service")
public interface PayServiceClient {
    @PostMapping("/api/pay/process-payment")
    PaymentStatus processPayment(@RequestParam("orderId") Long orderId,
                                 @RequestParam("userId") Long userId);

    @PostMapping("/api/pay/initiate")
    void initiatePayment(@RequestParam("orderId") Long orderId,
                         @RequestParam("userId") Long userId);

}
