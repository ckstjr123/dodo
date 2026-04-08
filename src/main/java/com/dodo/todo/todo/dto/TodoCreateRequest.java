package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.reminder.domain.ReminderType;
import com.dodo.todo.todo.repeat.domain.TodoRepeatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record TodoCreateRequest(
        @NotNull
        Long categoryId,

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 1000)
        String memo,

        @NotBlank
        @Size(max = 20)
        String priority,

        Integer sortOrder,

        LocalDateTime dueAt,

        List<@NotNull Long> tagIds,

        List<@Valid ChecklistRequest> checklists,

        @Valid
        RepeatRequest repeat,

        List<@Valid ReminderRequest> reminders
) {

    public record ChecklistRequest(
            @NotBlank
            @Size(max = 255)
            String content
    ) {
    }

    public record RepeatRequest(
            @NotNull
            TodoRepeatType repeatType,

            @Min(1)
            int repeatInterval,

            Set<DayOfWeek> daysOfWeek
    ) {
    }

    public record ReminderRequest(
            @NotNull
            ReminderType reminderType,

            Integer remindBefore,

            LocalDateTime remindAt
    ) {
    }
}
