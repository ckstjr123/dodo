package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.recurrencerule.Day;
import com.dodo.todo.recurrencerule.WeekDays;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

import java.util.Calendar;
import java.util.List;

@Schema(description = "요일 반복 조건")
public record ByDayRequest(
        @Schema(description = "월 반복의 n번째 주차. WEEKLY에서는 null", example = "1", minimum = "1", maximum = "5", nullable = true)
        @Range(min = 1, max = 5)
        Integer offset,

        @ArraySchema(
                schema = @Schema(description = "요일", example = "MO", allowableValues = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"}),
                minItems = 1,
                maxItems = Calendar.DAY_OF_WEEK
        )
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
