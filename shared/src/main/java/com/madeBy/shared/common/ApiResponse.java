package com.madeBy.shared.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 제외하고 반환
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ApiError error;

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

    @Getter
    public static class ApiError {
        private final String code;
        private final String message;

        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
