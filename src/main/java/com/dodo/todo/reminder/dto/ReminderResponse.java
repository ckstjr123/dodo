package com.dodo.todo.reminder.dto;

import com.dodo.todo.reminder.domain.Reminder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "미리 알림 응답")
public record ReminderResponse(
        @Schema(description = "알림 ID", example = "1")
        Long reminderId,

        @Schema(description = "일정 시각 기준 몇 분 전 알림인지", example = "10")
        int minuteOffset,

        @Schema(description = "실제 알림 예정 시각", example = "2026-05-12T08:50:00", type = "string", format = "date-time")
        LocalDateTime remindAt
) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getMinuteOffset(),
                reminder.getRemindAt()
        );
    }
}
