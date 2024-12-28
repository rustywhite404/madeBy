package com.madeby.cartservice.client;

import com.madeby.cartservice.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/users/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") Long userId);
}