package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TodoRequest(
        @NotNull
        Long categoryId,

        Long parentTodoId,

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 1000)
        String memo,

        Integer sortOrder,

        LocalDateTime dueAt,

        LocalDate scheduledDate,

        LocalTime scheduledTime,

        @Valid
        RecurrenceRuleRequest recurrenceRule
) {

    public RecurrenceRule getRecurrenceRule() {
        if (recurrenceRule == null) {
            return null;
        }

        return recurrenceRule.toRecurrenceRule();
    }

}
