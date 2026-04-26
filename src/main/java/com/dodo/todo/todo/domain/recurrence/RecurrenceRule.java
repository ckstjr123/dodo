package com.dodo.todo.todo.domain.recurrence;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Objects;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;

public record RecurrenceRule(
        Frequency frequency,
        int interval,
        @JsonSerialize(using = WeekDaysSerializer.class)
        @JsonDeserialize(using = WeekDaysDeserializer.class)
        WeekDays byDay,
        Integer byMonthDay,
        LocalDate until
) {

    private static final Map<WeekDay.Day, DayOfWeek> DAYS = Map.of(
            WeekDay.Day.MO, DayOfWeek.MONDAY,
            WeekDay.Day.TU, DayOfWeek.TUESDAY,
            WeekDay.Day.WE, DayOfWeek.WEDNESDAY,
            WeekDay.Day.TH, DayOfWeek.THURSDAY,
            WeekDay.Day.FR, DayOfWeek.FRIDAY,
            WeekDay.Day.SA, DayOfWeek.SATURDAY,
            WeekDay.Day.SU, DayOfWeek.SUNDAY
    );

    public RecurrenceRule {
        if (byDay == null) {
            byDay = WeekDays.empty();
        }
        validate(frequency, interval, byDay, byMonthDay);
    }

    /**
     * 다음 반복일을 계산한다.
     * 다음 반복 후보가 없거나 종료일을 넘으면 null을 반환한다.
     */
    public LocalDate nextDate(LocalDate currentDate) {
        if (currentDate == null) {
            throw new IllegalArgumentException("Current date is required");
        }

        if (frequency == Frequency.MONTHLY && byMonthDay != null) {
            LocalDate nextMonth = currentDate.withDayOfMonth(1).plusMonths(interval);
            LocalDate nextDate = nextMonth.withDayOfMonth(Math.min(byMonthDay, nextMonth.lengthOfMonth()));
            return isAfterEndDate(nextDate) ? null : nextDate;
        }
        if (frequency == Frequency.MONTHLY && !byDay.isEmpty()) {
            LocalDate currentMonthStart = currentDate.withDayOfMonth(1);
            LocalDate nextMonthStart = currentDate.plusMonths(interval).withDayOfMonth(1);

            LocalDate nextDate = byDay.findEarliestDateAfter(
                    currentDate,
                    day -> monthlyByDayDate(currentMonthStart, day)
            ).orElseGet(() -> byDay.findEarliestDate(
                    day -> monthlyByDayDate(nextMonthStart, day)
            ).orElseThrow());

            return isAfterEndDate(nextDate) ? null : nextDate;
        }

        Recur<LocalDate> recur = new Recur<>(toRRuleText());
        return recur.getNextDate(currentDate, currentDate);
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

    private LocalDate monthlyByDayDate(LocalDate monthStart, WeekDay day) {
        int week = day.getOffset();
        DayOfWeek dayOfWeek = DAYS.get(day.getDay());
        LocalDate firstDay = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek));
        LocalDate nthDay = firstDay.plusWeeks(week - 1L);

        if (Objects.equals(nthDay.getMonth(), monthStart.getMonth())) {
            return nthDay;
        }

        return monthStart.with(TemporalAdjusters.lastInMonth(dayOfWeek));
    }

    private static void validate(Frequency frequency, int interval, WeekDays byDay, Integer byMonthDay) {
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency is required");
        }
        if (interval < 1) {
            throw new IllegalArgumentException("Interval must be greater than 0");
        }
        if (!byDay.isEmpty() && byMonthDay != null) {
            throw new IllegalArgumentException("ByDay and ByMonthDay cannot be used together");
        }

        switch (frequency) {
            case DAILY -> {
                if (!byDay.isEmpty() || byMonthDay != null) {
                    throw new IllegalArgumentException("Daily recurrence must not have detail values");
                }
            }
            case WEEKLY -> {
                if (byDay.isEmpty()) {
                    throw new IllegalArgumentException("Weekly recurrence requires RFC 5545 day values");
                }
                if (byDay.hasOrdinalWeekday()) {
                    throw new IllegalArgumentException("Weekly recurrence must not have offsets");
                }
            }
            case MONTHLY -> {
                if (byDay.isEmpty() && byMonthDay == null) {
                    throw new IllegalArgumentException("Monthly recurrence requires ByDay or ByMonthDay");
                }
                if (byMonthDay != null && (byMonthDay < 1 || byMonthDay > 31)) {
                    throw new IllegalArgumentException("ByMonthDay must be between 1 and 31");
                }
                if (!byDay.isEmpty() && byDay.hasOffsetOutOfRange(1, 5)) {
                    throw new IllegalArgumentException("Monthly byDay must be 1MO through 5SU");
                }
            }
        }
    }

}
