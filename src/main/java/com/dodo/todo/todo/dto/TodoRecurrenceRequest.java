package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.recurrence.RecurrenceCriteria;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TodoRecurrenceRequest(
        @NotNull
        @Valid
        RecurrenceRuleRequest rule,

        RecurrenceCriteria criteria
) {

    public TodoRecurrence toTodoRecurrence() {
        return new TodoRecurrence(rule.toRecurrenceRule(), criteria);
    }
}
