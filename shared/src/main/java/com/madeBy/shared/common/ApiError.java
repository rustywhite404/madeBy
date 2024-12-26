package com.madeBy.shared.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiError {
    private String code;
    private String message;
}
