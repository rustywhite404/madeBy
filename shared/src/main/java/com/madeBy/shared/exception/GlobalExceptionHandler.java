package com.madeBy.shared.exception;

import com.madeBy.shared.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("데이터 제약 조건 위반 - url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        // Duplicate entry 오류를 처리하기 위한 로직
        if (ex.getMessage().contains("Duplicate entry")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.failure(MadeByErrorCode.DUPLICATED_DATA.name(), MadeByErrorCode.DUPLICATED_DATA.getMessage()));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(MadeByErrorCode.INTERNAL_SERVER_ERROR.name(), ex.getMessage()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceUnavailableException(ServiceUnavailableException ex, HttpServletRequest req) {
        log.error("서비스 장애 - url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("SERVICE_UNAVAILABLE", ex.getMessage()));
    }


}
