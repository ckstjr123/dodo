package com.dodo.todo.category.domain;

import com.dodo.todo.common.exception.ErrorCode;

public enum CategoryError implements ErrorCode {

    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", 404, "Category not found"),
    CATEGORY_DUPLICATED("CATEGORY_DUPLICATED", 409, "Category already exists"),
    CATEGORY_IN_USE("CATEGORY_IN_USE", 400, "Category is in use");

    private final String code;
    private final int status;
    private final String message;

    CategoryError(String code, int status, String message) {
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
