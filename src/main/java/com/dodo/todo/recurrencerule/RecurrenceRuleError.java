package com.dodo.todo.recurrencerule;

public enum RecurrenceRuleError {

    FREQUENCY_REQUIRED("Frequency is required"),
    INTERVAL_NOT_POSITIVE("Interval must be greater than 0"),
    CURRENT_DATE_REQUIRED("Current date is required"),
    WEEK_DAYS_EMPTY("WeekDays is empty");

    private final String message;

    RecurrenceRuleError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
