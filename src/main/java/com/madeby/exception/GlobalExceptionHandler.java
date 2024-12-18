package com.madeby.exception;

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
    public ResponseEntity<MadeByErrorResponse> handleMyReviewServiceException(MadeByException ex, HttpServletRequest req){
        log.error("errorCode: {}, url: {}, message: {}", ex.getMadeByErrorCode(), req.getRequestURI(), ex.getDetailMessage());

        //응답 DTO 설정
        MadeByErrorResponse response = MadeByErrorResponse.builder()
                .errorCode(ex.getMadeByErrorCode())
                .errorMessage(ex.getDetailMessage())
                .build();

        //HTTP 응답 상태 설정
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    //잘못된 클라이언트 요청일 경우(GET 대신 POST로 요청이 왔을 때, 유효성 검사를 통과하지 못했을 때 등)
    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class, MethodArgumentNotValidException.class})
    public MadeByErrorResponse handleBadRequest(Exception ex, HttpServletRequest req){
        log.error("url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return MadeByErrorResponse.builder()
                .errorCode(MadeByErrorCode.INVALID_REQUEST)
                .errorMessage(MadeByErrorCode.INVALID_REQUEST.getMessage())
                .build();
    }

    //기타 서버 장애가 발생한 경우
    @ExceptionHandler(Exception.class)
    public MadeByErrorResponse handleException(Exception ex, HttpServletRequest req){
        log.error("url: {}, message: {}", req.getRequestURI(), ex.getMessage());

        return MadeByErrorResponse.builder()
                .errorCode(MadeByErrorCode.INTERNAL_SERVER_ERROR)
                .errorMessage(MadeByErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .build();
    }

}
