package com.dodo.todo.todo.domain;

import org.springframework.http.HttpStatus;

public enum TodoError {

    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Category not found"),
    TODO_NOT_FOUND("TODO_NOT_FOUND", HttpStatus.NOT_FOUND, "Todo not found"),
    TODO_DEPTH_LIMIT_EXCEEDED("TODO_DEPTH_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "Todo depth must not exceed 2"),
    INVALID_CURSOR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Cursor completedAt and cursorId must be provided together"),
    MAIN_TODO_NOT_OWNED("MAIN_TODO_NOT_OWNED", HttpStatus.BAD_REQUEST, "Main todo must belong to the same member"),
    TODO_ALREADY_COMPLETED("TODO_ALREADY_COMPLETED", HttpStatus.BAD_REQUEST, "Todo already completed"),
    COMPLETED_DATE_REQUIRED("COMPLETED_DATE_REQUIRED", HttpStatus.BAD_REQUEST, "Completed date is required"),
    TODO_NOT_COMPLETED("TODO_NOT_COMPLETED", HttpStatus.BAD_REQUEST, "Todo is not completed"),
    TODO_STATUS_REQUIRED("TODO_STATUS_REQUIRED", HttpStatus.BAD_REQUEST, "Todo status is required"),
    RECURRING_TODO_SCHEDULED_DATE_REQUIRED(
            "RECURRING_TODO_SCHEDULED_DATE_REQUIRED",
            HttpStatus.BAD_REQUEST,
            "Scheduled date is required for recurring todo"
    );

    private final String code;
    private final HttpStatus status;
    private final String message;

    TodoError(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
