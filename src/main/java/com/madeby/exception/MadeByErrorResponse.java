package com.madeby.exception;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MadeByErrorResponse {
    private MadeByErrorCode errorCode;
    private String errorMessage;
}
