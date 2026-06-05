package com.dodo.todo.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "미리 알림 수정 요청")
public record ReminderUpdateRequest(
        @Schema(description = "일정 시각 기준 몇 분 전에 알림을 보낼지", example = "10", nullable = true)
        Integer minuteOffset,

        @Schema(description = "절대 알림 일시", example = "2026-05-20T09:00:00", nullable = true)
        LocalDateTime due
) {
}
