package com.dodo.todo.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reminder create response")
public record ReminderCreateResponse(
        @Schema(description = "Reminder ID", example = "1")
        Long reminderId
) {
}
