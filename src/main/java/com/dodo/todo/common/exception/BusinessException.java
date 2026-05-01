package com.dodo.todo.common.exception;

public class BusinessException extends RuntimeException {

    private final String code;
    private final int status;

    public BusinessException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.status(), errorCode.message());
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
