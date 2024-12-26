package com.madeBy.shared.exception;

import com.madeby.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MadeByException.class)
    public ResponseEntity<ApiResponse<?>> handleMadeByException(MadeByException ex, HttpServletRequest req) {
        log.warn("errorCode: {}, url: {}, message: {}", ex.getMadeByErrorCode(), req.getRequestURI(), ex.getDetailMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMadeByErrorCode().name(), ex.getDetailMessage()));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<?>> handleBadRequest(Exception ex, HttpServletRequest req) {
        log.info("잘못된 클라이언트 요청 - url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(MadeByErrorCode.INVALID_REQUEST.name(), MadeByErrorCode.INVALID_REQUEST.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex, HttpServletRequest req) {
        log.error("예상치 못한 예외 - url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(MadeByErrorCode.INTERNAL_SERVER_ERROR.name(), MadeByErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex) {
        log.info("인증 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("AUTHENTICATION_FAILED", ex.getMessage()));
    }
}
