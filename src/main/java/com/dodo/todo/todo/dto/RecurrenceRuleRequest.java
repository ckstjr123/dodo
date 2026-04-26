package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.domain.recurrence.WeekDays;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import net.fortuna.ical4j.model.WeekDay;
import org.springframework.http.HttpStatus;

public record RecurrenceRuleRequest(
        Frequency frequency,
        int interval,
        @Size(max = 7)
        List<String> byDay,
        Integer byMonthDay,
        LocalDate until
) {

    public RecurrenceRule toRecurrenceRule() {
        try {
            return new RecurrenceRule(
                    frequency,
                    interval,
                    WeekDays.from(byDay == null ? List.of() : byDay.stream().map(WeekDay::new).toList()),
                    byMonthDay,
                    until
            );
        } catch (IllegalArgumentException exception) {
            throw new ApiException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
