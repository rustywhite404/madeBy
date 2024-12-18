package com.madeby.common;

import lombok.Getter;

@Getter
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