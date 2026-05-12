package com.dodo.todo.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "미리 알림 요청")
public record ReminderRequest(
        @Schema(description = "일정 시각 기준 몇 분 전 알림인지", example = "10")
        @NotNull
        @PositiveOrZero
        Integer minuteOffset
) {
}
