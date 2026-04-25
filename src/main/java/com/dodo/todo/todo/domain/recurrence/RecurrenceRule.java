package com.dodo.todo.todo.domain.recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import net.fortuna.ical4j.model.Recur;

public record RecurrenceRule(
        Frequency frequency,
        int interval,
        List<String> byDay,
        Integer byMonthDay,
        LocalDate until
) {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "MO", DayOfWeek.MONDAY,
            "TU", DayOfWeek.TUESDAY,
            "WE", DayOfWeek.WEDNESDAY,
            "TH", DayOfWeek.THURSDAY,
            "FR", DayOfWeek.FRIDAY,
            "SA", DayOfWeek.SATURDAY,
            "SU", DayOfWeek.SUNDAY
    );

    private static final Pattern MONTHLY_BY_DAY = Pattern.compile("[1-5](MO|TU|WE|TH|FR|SA|SU)");

    public RecurrenceRule {
        byDay = byDay == null ? List.of() : byDay.stream()
                .map(day -> day.toUpperCase(Locale.ROOT))
                .toList();
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
            return isAfterUntil(nextDate) ? null : nextDate;
        }
        if (frequency == Frequency.MONTHLY && !byDay.isEmpty()) {
            LocalDate currentMonthStart = currentDate.withDayOfMonth(1);
            LocalDate nextMonthStart = currentDate.plusMonths(interval).withDayOfMonth(1);

            LocalDate nextDate = byDay.stream()
                    .map(day -> monthlyByDayDate(currentMonthStart, day))
                    .filter(date -> date.isAfter(currentDate))
                    .min(LocalDate::compareTo)
                    .orElseGet(() -> byDay.stream()
                            .map(day -> monthlyByDayDate(nextMonthStart, day))
                            .min(LocalDate::compareTo)
                            .orElseThrow());

            return isAfterUntil(nextDate) ? null : nextDate;
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
            rule.append(";BYDAY=").append(String.join(",", byDay));
        }
        if (byMonthDay != null) {
            rule.append(";BYMONTHDAY=").append(byMonthDay);
        }
        if (until != null) {
            rule.append(";UNTIL=").append(until.format(DateTimeFormatter.BASIC_ISO_DATE));
        }

        return rule.toString();
    }

    private boolean isAfterUntil(LocalDate date) {
        return until != null && date.isAfter(until);
    }

    private LocalDate monthlyByDayDate(LocalDate monthStart, String day) {
        int week = Character.getNumericValue(day.charAt(0));
        DayOfWeek dayOfWeek = DAYS.get(day.substring(1));
        LocalDate firstDay = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek));
        LocalDate nthDay = firstDay.plusWeeks(week - 1L);

        if (Objects.equals(nthDay.getMonth(), monthStart.getMonth())) {
            return nthDay;
        }

        return monthStart.with(TemporalAdjusters.lastInMonth(dayOfWeek));
    }

    private static void validate(Frequency frequency, int interval, List<String> byDay, Integer byMonthDay) {
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
                if (byDay.stream().anyMatch(day -> !DAYS.containsKey(day))) {
                    throw new IllegalArgumentException("Weekly recurrence requires RFC 5545 day values");
                }
            }
            case MONTHLY -> {
                if (byDay.isEmpty() && byMonthDay == null) {
                    throw new IllegalArgumentException("Monthly recurrence requires ByDay or ByMonthDay");
                }
                if (byMonthDay != null && (byMonthDay < 1 || byMonthDay > 31)) {
                    throw new IllegalArgumentException("ByMonthDay must be between 1 and 31");
                }
                if (!byDay.isEmpty() && byDay.stream().anyMatch(day -> !MONTHLY_BY_DAY.matcher(day).matches())) {
                    throw new IllegalArgumentException("Monthly byDay must be 1MO through 5SU");
                }
            }
        }
    }

}
