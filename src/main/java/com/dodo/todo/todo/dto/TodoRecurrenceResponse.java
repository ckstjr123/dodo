package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.recurrence.RecurrenceCriteria;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "할 일 반복 설정 응답")
public record TodoRecurrenceResponse(
        @Schema(description = "반복 규칙")
        RecurrenceRuleResponse rule,

        @Schema(description = "반복 기준", example = "SCHEDULED_DATE", allowableValues = {"SCHEDULED_DATE", "COMPLETED_DATE"})
        RecurrenceCriteria criteria
) {

    public static TodoRecurrenceResponse from(TodoRecurrence recurrence) {
        if (recurrence == null) {
            return null;
        }

        return new TodoRecurrenceResponse(
                RecurrenceRuleResponse.from(recurrence.rule()),
                recurrence.criteria()
        );
    }
}
