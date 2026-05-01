package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.recurrence.RecurrenceCriteria;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;

public record TodoRecurrenceResponse(
        RecurrenceRuleResponse rule,
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
