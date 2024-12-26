package com.madeby.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) //null값 제외하고 반환
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<?> failure(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }
}