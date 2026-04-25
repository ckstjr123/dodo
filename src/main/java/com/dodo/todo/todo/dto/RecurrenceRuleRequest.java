package com.dodo.todo.todo.dto;

import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;

public record RecurrenceRuleRequest(
        Frequency frequency,
        int interval,
        List<String> byDay,
        Integer byMonthDay,
        LocalDate until
) {

    public RecurrenceRule toRecurrenceRule() {
        try {
            return new RecurrenceRule(frequency, interval, byDay, byMonthDay, until);
        } catch (IllegalArgumentException exception) {
            throw new ApiException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
