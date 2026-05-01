package com.dodo.todo.todo.dto;

import com.dodo.todo.recurrencerule.Day;
import com.dodo.todo.recurrencerule.Frequency;
import com.dodo.todo.recurrencerule.RecurrenceRule;
import java.time.LocalDate;
import java.util.List;

public record RecurrenceRuleResponse(
        Frequency frequency,
        int interval,
        Integer offset,
        List<Day> days,
        Integer byMonthDay,
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
