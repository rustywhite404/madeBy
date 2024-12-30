package com.madeby.cartservice.client;

import com.madeby.cartservice.dto.UserDetailsDto;
import com.madeby.cartservice.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/user/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/api/user/validate-token")
    UserDetailsDto validateToken(@RequestHeader("Authorization") String token);
}