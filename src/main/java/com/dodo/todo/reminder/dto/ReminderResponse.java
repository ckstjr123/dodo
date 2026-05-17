package com.dodo.todo.reminder.dto;

import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.domain.ReminderType;
import com.dodo.todo.reminder.domain.AbsoluteReminder;
import com.dodo.todo.reminder.domain.RelativeReminder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "미리 알림 응답")
public record ReminderResponse(
        @Schema(description = "알림 ID", example = "1")
        Long reminderId,

        @Schema(description = "알림 타입", example = "RELATIVE")
        ReminderType type,

        @Schema(description = "일정 시각 기준 몇 분 전 알림인지", example = "10", nullable = true)
        Integer minuteOffset,

        @Schema(description = "절대 알림 일시", example = "2026-05-20T09:00:00", nullable = true)
        LocalDateTime due
) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getType(),
                reminder instanceof RelativeReminder relativeReminder ? relativeReminder.getMinuteOffset() : null,
                reminder instanceof AbsoluteReminder absoluteReminder ? absoluteReminder.getDue() : null
        );
    }
}
