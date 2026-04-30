package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.domain.recurrence.RecurrenceCriteria;
import com.dodo.todo.todo.domain.recurrence.WeekDays;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecurrenceRuleRequest(
        @NotNull
        Frequency frequency,

        @Min(1)
        int interval,

        @Valid
        ByDayRequest byDay,

        @Min(1)
        @Max(31)
        Integer byMonthDay,

        LocalDate until,

        RecurrenceCriteria criteria
) {

    public RecurrenceRule toRecurrenceRule() {
        validate();

        return new RecurrenceRule(
                frequency,
                interval,
                toWeekDays(),
                byMonthDay,
                until,
                criteria
        );
    }

    private WeekDays toWeekDays() {
        if (!hasByDay()) {
            return WeekDays.empty();
        }

        return byDay.toWeekDays();
    }

    private void validate() {
        if (hasByDay() && hasByMonthDay()) {
            throwValidationError(RecurrenceRuleRequestError.BY_DAY_AND_BY_MONTH_DAY_TOGETHER);
        }

        switch (frequency) {
            case DAILY -> {
                if (hasByDay() || hasByMonthDay()) {
                    throwValidationError(RecurrenceRuleRequestError.DAILY_DETAIL_VALUES);
                }
            }
            case WEEKLY -> {
                if (!hasByDay()) {
                    throwValidationError(RecurrenceRuleRequestError.WEEKLY_BY_DAY_REQUIRED);
                }
                byDay.validateWeekly();
            }
            case MONTHLY -> {
                if (!hasByDay() && !hasByMonthDay()) {
                    throwValidationError(RecurrenceRuleRequestError.MONTHLY_DETAIL_REQUIRED);
                }
                if (hasByDay()) {
                    byDay.validateMonthly();
                }
            }
        }
    }

    private void throwValidationError(RecurrenceRuleRequestError error) {
        throw new ApiException(error.code(), error.status(), error.message());
    }

    private boolean hasByDay() {
        return byDay != null;
    }

    private boolean hasByMonthDay() {
        return byMonthDay != null;
    }
}
