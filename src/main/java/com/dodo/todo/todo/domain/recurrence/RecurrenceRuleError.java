package com.dodo.todo.todo.domain.recurrence;

public enum RecurrenceRuleError {

    FREQUENCY_REQUIRED("Frequency is required"),
    INTERVAL_NOT_POSITIVE("Interval must be greater than 0"),
    COMPLETION_BEFORE_SCHEDULED_DATE("Completion-based recurring todos cannot be completed until the actual date arrives"),
    CURRENT_DATE_REQUIRED("Current date is required"),
    WEEK_DAYS_EMPTY("WeekDays is empty"),
    INVALID_RECURRENCE_RULE("Invalid recurrence rule");

    private final String message;

    RecurrenceRuleError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
