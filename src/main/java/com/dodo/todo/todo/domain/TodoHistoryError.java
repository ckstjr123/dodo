package com.dodo.todo.todo.domain;

import com.dodo.todo.common.exception.ErrorCode;

public enum TodoHistoryError implements ErrorCode {

    TODO_REQUIRED("TODO_REQUIRED", 400, "Todo is required"),
    TODO_TITLE_REQUIRED("TODO_TITLE_REQUIRED", 400, "Todo title is required"),
    MEMBER_REQUIRED("MEMBER_REQUIRED", 400, "Member is required"),
    COMPLETED_AT_REQUIRED("COMPLETED_AT_REQUIRED", 400, "Completed at is required");

    private final String code;
    private final int status;
    private final String message;

    TodoHistoryError(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }
}
