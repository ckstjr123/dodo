package com.dodo.todo.todo.dto;

import com.dodo.todo.recurrencerule.Day;
import com.dodo.todo.recurrencerule.Frequency;
import com.dodo.todo.recurrencerule.RecurrenceRule;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "반복 규칙 응답")
public record RecurrenceRuleResponse(
        @Schema(description = "반복 주기", example = "DAILY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY"})
        Frequency frequency,

        @Schema(description = "반복 간격", example = "1")
        int interval,

        @Schema(description = "월 반복의 n번째 주차. 없으면 null", example = "1", nullable = true)
        Integer offset,

        @ArraySchema(schema = @Schema(description = "반복 요일", example = "MO", allowableValues = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"}))
        List<Day> days,

        @Schema(description = "월 반복 날짜. 없으면 null", example = "15", nullable = true)
        Integer byMonthDay,

        @Schema(description = "반복 종료 날짜", example = "2026-05-10", type = "string", format = "date", nullable = true)
        LocalDate until
) {

    public static RecurrenceRuleResponse from(RecurrenceRule recurrenceRule) {
        if (recurrenceRule == null) {
            return null;
        }

        return new RecurrenceRuleResponse(
                recurrenceRule.frequency(),
                recurrenceRule.interval(),
                recurrenceRule.byDay().isEmpty() ? null : recurrenceRule.byDay().offset(),
                recurrenceRule.byDay().isEmpty() ? List.of() : recurrenceRule.byDay().days(),
                recurrenceRule.byMonthDay(),
                recurrenceRule.until()
        );
    }
}
