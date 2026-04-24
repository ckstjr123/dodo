package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TodoCreateRequest(
        @NotNull
        Long categoryId,

        Long mainTodoId,

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 1000)
        String memo,

        Integer sortOrder,

        LocalDateTime dueAt,

        LocalDate scheduledDate,

        LocalTime scheduledTime,

        RecurrenceRule recurrenceRule
) {
}
