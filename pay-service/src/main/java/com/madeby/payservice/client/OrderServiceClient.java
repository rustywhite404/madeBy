package com.madeby.payservice.client;
import com.madeby.payservice.dto.OrderRequestDto;
import com.madeby.payservice.dto.OrderResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceClient {
    @GetMapping("/api/orders/{orderId}")
    OrderResponseDto getOrderDetails(@PathVariable Long orderId);
}
