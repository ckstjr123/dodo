package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.recurrence.RecurrenceCriteria;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "할 일 반복 설정")
public record TodoRecurrenceRequest(
        @Schema(description = "반복 규칙", implementation = RecurrenceRuleRequest.class)
        @NotNull
        @Valid
        RecurrenceRuleRequest rule,

        @Schema(description = "반복 기준", example = "SCHEDULED_DATE", allowableValues = {"SCHEDULED_DATE", "COMPLETED_DATE"}, nullable = true)
        RecurrenceCriteria criteria
) {

    public TodoRecurrence toTodoRecurrence() {
        return new TodoRecurrence(rule.toRecurrenceRule(), criteria);
    }
}
