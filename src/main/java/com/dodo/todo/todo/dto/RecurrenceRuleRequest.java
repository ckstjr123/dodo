package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.recurrencerule.Frequency;
import com.dodo.todo.recurrencerule.RecurrenceRule;
import com.dodo.todo.recurrencerule.WeekDays;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "반복 규칙 요청")
public record RecurrenceRuleRequest(
        @Schema(description = "반복 주기. DAILY는 byDay/byMonthDay 없이 사용", example = "DAILY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY"})
        @NotNull
        Frequency frequency,

        @Schema(description = "반복 간격. 1 이상", example = "1", minimum = "1")
        @Min(1)
        int interval,

        @Schema(description = "요일 반복 조건. WEEKLY는 필수, MONTHLY는 offset과 단일 요일 필요, DAILY는 null", nullable = true)
        @Valid
        ByDayRequest byDay,

        @Schema(description = "월 반복 날짜. MONTHLY에서 1~31 사용, DAILY/WEEKLY는 null", example = "15", minimum = "1", maximum = "31", nullable = true)
        @Min(1)
        @Max(31)
        Integer byMonthDay,

        @Schema(description = "반복 종료 날짜", example = "2026-05-10", type = "string", format = "date", nullable = true)
        LocalDate until
) {

    public RecurrenceRule toRecurrenceRule() {
        validate();

        return new RecurrenceRule(
                frequency,
                interval,
                toWeekDays(),
                byMonthDay,
                until
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
        throw new BusinessException(error);
    }

    private boolean hasByDay() {
        return byDay != null;
    }

    private boolean hasByMonthDay() {
        return byMonthDay != null;
    }
}
