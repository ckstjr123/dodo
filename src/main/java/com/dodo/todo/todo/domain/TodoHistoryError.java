package com.dodo.todo.todo.domain;

public enum TodoHistoryError {

    TODO_REQUIRED("Todo is required"),
    TODO_TITLE_REQUIRED("Todo title is required"),
    MEMBER_REQUIRED("Member is required"),
    COMPLETED_AT_REQUIRED("Completed at is required");

    private final String message;

    TodoHistoryError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
