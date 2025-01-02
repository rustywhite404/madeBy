package com.madeby.apigateway.exception;

import com.madeBy.shared.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex, ServerWebExchange exchange) {
        // failure 메서드를 사용해 에러 응답 생성
        ApiResponse<?> response = ApiResponse.failure("GATEWAY_ERROR", "게이트웨이에서 에러가 발생했습니다.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}