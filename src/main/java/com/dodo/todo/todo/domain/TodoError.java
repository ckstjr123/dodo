package com.dodo.todo.todo.domain;

import com.dodo.todo.common.exception.ErrorCode;

public enum TodoError implements ErrorCode {

    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", 404, "Category not found"),
    TODO_NOT_FOUND("TODO_NOT_FOUND", 404, "Todo not found"),
    TODO_DEPTH_LIMIT_EXCEEDED("TODO_DEPTH_LIMIT_EXCEEDED", 400, "Todo depth must not exceed 2"),
    INVALID_CURSOR("VALIDATION_ERROR", 400, "Cursor completedAt and cursorId must be provided together"),
    MAIN_TODO_NOT_OWNED("MAIN_TODO_NOT_OWNED", 400, "Main todo must belong to the same member"),
    TODO_ALREADY_COMPLETED("TODO_ALREADY_COMPLETED", 400, "Todo already completed"),
    COMPLETED_DATE_REQUIRED("COMPLETED_DATE_REQUIRED", 400, "Completed date is required"),
    TODO_NOT_COMPLETED("TODO_NOT_COMPLETED", 400, "Todo is not completed"),
    TODO_STATUS_REQUIRED("TODO_STATUS_REQUIRED", 400, "Todo status is required"),
    RECURRING_TODO_SCHEDULED_DATE_REQUIRED(
            "RECURRING_TODO_SCHEDULED_DATE_REQUIRED",
            400,
            "Scheduled date is required for recurring todo"
    );

    private final String code;
    private final int status;
    private final String message;

    TodoError(String code, int status, String message) {
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
