package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.recurrencerule.Day;
import com.dodo.todo.recurrencerule.WeekDays;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

import java.util.Calendar;
import java.util.List;

public record ByDayRequest(
        @Range(min = 1, max = 5)
        Integer offset,

        @NotEmpty
        @Size(max = Calendar.DAY_OF_WEEK)
        List<@NotNull Day> days
) {

    WeekDays toWeekDays() {
        return WeekDays.of(offset == null ? 0 : offset, days);
    }

    void validateWeekly() {
        if (hasOffset()) {
            throwValidationError(RecurrenceRuleRequestError.WEEKLY_OFFSET_NOT_ALLOWED);
        }
    }

    void validateMonthly() {
        if (!hasOffset()) {
            throwValidationError(RecurrenceRuleRequestError.MONTHLY_BY_DAY_OFFSET_REQUIRED);
        }
        if (days.size() > 1) {
            throwValidationError(RecurrenceRuleRequestError.MONTHLY_BY_DAY_SINGLE_DAY_REQUIRED);
        }
    }

    private boolean hasOffset() {
        return offset != null && offset != 0;
    }

    private void throwValidationError(RecurrenceRuleRequestError error) {
        throw new BusinessException(error);
    }
}
