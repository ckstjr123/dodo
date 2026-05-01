package com.dodo.todo.todo.domain.recurrence;

import com.dodo.todo.recurrencerule.RecurrenceRule;
import java.time.LocalDate;
import java.util.Optional;

public record TodoRecurrence(
        RecurrenceRule rule,
        RecurrenceCriteria criteria
) {

    public TodoRecurrence {
        if (rule == null) {
            throw new IllegalArgumentException("Recurrence rule is required");
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
                throw new IllegalStateException("Completion-based recurring todos cannot be completed until the actual date arrives");
            }
            return rule.nextDate(completedDate);
        }

        return rule.nextDate(scheduledDate);
    }
}
