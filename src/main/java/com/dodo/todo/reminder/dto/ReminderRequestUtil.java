package com.dodo.todo.reminder.dto;

final class ReminderRequestUtil {

    private static final int MISSING_MINUTE_OFFSET = -1;

    private ReminderRequestUtil() {
    }

    static Integer resolveMinuteOffset(Integer minuteOffset) {
        return minuteOffset == null ? MISSING_MINUTE_OFFSET : minuteOffset;
    }
}
