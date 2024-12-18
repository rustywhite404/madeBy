package com.madeby.exception;

import lombok.Getter;

@Getter
public class MadeByException extends RuntimeException{
    private MadeByErrorCode madeByErrorCode;
    private String detailMessage;

    public MadeByException(MadeByErrorCode errorCode){
        super(errorCode.getMessage());
        this.madeByErrorCode = errorCode;
        this.detailMessage = errorCode.getMessage();
    }
    public MadeByException(MadeByErrorCode errorCode, String detailMessage){
        super(detailMessage);
        this.madeByErrorCode = errorCode;
        this.detailMessage = detailMessage;
    }
}
