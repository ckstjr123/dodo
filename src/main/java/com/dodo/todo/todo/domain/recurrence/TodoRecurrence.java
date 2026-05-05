package com.dodo.todo.todo.domain.recurrence;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.recurrencerule.RecurrenceRule;
import com.dodo.todo.todo.domain.TodoError;
import java.time.LocalDate;
import java.util.Optional;

public record TodoRecurrence(
        RecurrenceRule rule,
        RecurrenceCriteria criteria
) {

    public TodoRecurrence {
        if (rule == null) {
            throw new BusinessException(TodoError.RECURRENCE_RULE_REQUIRED);
        }
        if (criteria == null) {
            criteria = RecurrenceCriteria.SCHEDULED_DATE;
        }
    }

    /**
     * Todo 반복 날짜 기준에 따라 다음 반복일을 계산함.
     */
    public Optional<LocalDate> nextDate(LocalDate scheduledDate, LocalDate completedDate) {
        if (criteria == RecurrenceCriteria.COMPLETED_DATE) {
            if (scheduledDate.isAfter(completedDate)) {
                throw new BusinessException(TodoError.COMPLETION_BASED_RECURRING_TODO_NOT_DUE);
            }
            return rule.getNextDate(completedDate);
        }

        return rule.getNextDate(scheduledDate);
    }
}
