package com.dodo.todo.recurrencerule;

import net.fortuna.ical4j.model.Recur;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public record RecurrenceRule(
        Frequency frequency,
        int interval,
        WeekDays byDay,
        Integer byMonthDay,
        LocalDate until
) {

    public RecurrenceRule {
        if (frequency == null) {
            throw new IllegalArgumentException(RecurrenceRuleError.FREQUENCY_REQUIRED.message());
        }
        if (interval < 1) {
            throw new IllegalArgumentException(RecurrenceRuleError.INTERVAL_NOT_POSITIVE.message());
        }
        if (byDay == null) {
            byDay = WeekDays.empty();
        }
    }

    /**
     * 다음 반복일을 계산한다.
     * 다음 반복 후보가 없거나 종료일을 넘으면 empty Optional을 반환한다.
     */
    public Optional<LocalDate> getNextDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException(RecurrenceRuleError.CURRENT_DATE_REQUIRED.message());
        }

        if (frequency == Frequency.MONTHLY && byMonthDay != null) {
            LocalDate targetMonth = date.withDayOfMonth(1).plusMonths(interval);
            LocalDate nextDate =  targetMonth.withDayOfMonth(Math.min(byMonthDay, targetMonth.lengthOfMonth()));
            return isAfterEndDate(nextDate) ? Optional.empty() : Optional.of(nextDate);
        }
        if (frequency == Frequency.MONTHLY && !byDay.isEmpty()) {
            Optional<LocalDate> sameMonthNextDate = byDay.nextDateAfterInMonth(date);
            if (sameMonthNextDate.isPresent()) {
                LocalDate nextDate = sameMonthNextDate.get();
                return isAfterEndDate(nextDate) ? Optional.empty() : Optional.of(nextDate);
            }

            LocalDate targetMonth = date.withDayOfMonth(1).plusMonths(interval);
            LocalDate nextDate = byDay.nextDateInMonth(targetMonth);
            return isAfterEndDate(nextDate) ? Optional.empty() : Optional.of(nextDate);
        }

        Recur<LocalDate> recur = new Recur<>(toRRuleText());
        return Optional.ofNullable(recur.getNextDate(date, date));
    }

    public String toRRuleText() {
        StringBuilder rule = new StringBuilder("FREQ=")
                .append(frequency)
                .append(";INTERVAL=")
                .append(interval);

        if (!byDay.isEmpty()) {
            rule.append(";BYDAY=").append(byDay.toRRuleValue());
        }
        if (byMonthDay != null) {
            rule.append(";BYMONTHDAY=").append(byMonthDay);
        }
        if (until != null) {
            rule.append(";UNTIL=").append(until.format(DateTimeFormatter.BASIC_ISO_DATE));
        }

        return rule.toString();
    }

    private boolean isAfterEndDate(LocalDate date) {
        return until != null && date.isAfter(until);
    }

}
