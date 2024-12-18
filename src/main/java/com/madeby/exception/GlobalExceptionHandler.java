package com.madeby.exception;
import com.madeby.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
        log.error("errorCode: {}, url: {}, message: {}", ex.getMadeByErrorCode(), req.getRequestURI(), ex.getDetailMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMadeByErrorCode().name(), ex.getDetailMessage()));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<?>> handleBadRequest(Exception ex, HttpServletRequest req) {
        log.error("url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(MadeByErrorCode.INVALID_REQUEST.name(), MadeByErrorCode.INVALID_REQUEST.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex, HttpServletRequest req) {
        log.error("url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(MadeByErrorCode.INTERNAL_SERVER_ERROR.name(), MadeByErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
