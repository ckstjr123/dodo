package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.ErrorCode;

public enum RecurrenceRuleRequestError implements ErrorCode {

    BY_DAY_AND_BY_MONTH_DAY_TOGETHER("byDay and byMonthDay cannot be used together"),
    DAILY_DETAIL_VALUES("Daily recurrence must not have detail values"),
    WEEKLY_BY_DAY_REQUIRED("Weekly recurrence requires RFC 5545 day values"),
    WEEKLY_OFFSET_NOT_ALLOWED("Weekly recurrence must not have offsets"),
    MONTHLY_DETAIL_REQUIRED("Monthly recurrence requires days or byMonthDay"),
    MONTHLY_BY_DAY_OFFSET_REQUIRED("Monthly byDay must be 1MO through 5SU"),
    MONTHLY_BY_DAY_SINGLE_DAY_REQUIRED("Monthly byDay requires exactly one day");

    private final String message;

    RecurrenceRuleRequestError(String message) {
        this.message = message;
    }

    public String code() {
        return "VALIDATION_ERROR";
    }

    public int status() {
        return 400;
    }

    public String message() {
        return message;
    }
}
