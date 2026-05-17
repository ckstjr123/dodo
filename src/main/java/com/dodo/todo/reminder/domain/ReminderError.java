package com.dodo.todo.reminder.domain;

import com.dodo.todo.common.exception.ErrorCode;

public enum ReminderError implements ErrorCode {

    REMINDER_NOT_FOUND("REMINDER_NOT_FOUND", 404, "Reminder not found"),
    REMINDER_SCHEDULE_REQUIRED("REMINDER_SCHEDULE_REQUIRED", 400, "Scheduled date and time are required for reminder"),
    REMINDER_LIMIT_EXCEEDED("REMINDER_LIMIT_EXCEEDED", 400, "Reminder count must not exceed 5"),
    REMINDER_TYPE_FIELD_INVALID("REMINDER_TYPE_FIELD_INVALID", 400, "Reminder fields do not match reminder type"),
    REMINDER_DUE_REQUIRED("REMINDER_DUE_REQUIRED", 400, "Reminder due is required"),
    REMINDER_OFFSET_NEGATIVE("REMINDER_OFFSET_NEGATIVE", 400, "Reminder minute offset must be zero or positive"),
    REMINDER_OFFSET_DUPLICATED("REMINDER_OFFSET_DUPLICATED", 400, "Reminder minute offset already exists"),
    REMINDER_DUE_DUPLICATED("REMINDER_DUE_DUPLICATED", 400, "Reminder due already exists");

    private final String code;
    private final int status;
    private final String message;

    ReminderError(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
