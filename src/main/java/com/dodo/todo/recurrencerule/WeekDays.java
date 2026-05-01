package com.dodo.todo.recurrencerule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record WeekDays(
        int offset,
        List<Day> days
) {

    private static final WeekDays EMPTY = new WeekDays(0, List.of());

    public static WeekDays empty() {
        return EMPTY;
    }

    public static WeekDays of(int offset, List<Day> days) {
        if (days == null || days.isEmpty()) {
            return empty();
        }

        return new WeekDays(offset, Collections.unmodifiableList(days));
    }

    public boolean isEmpty() {
        return days.isEmpty();
    }

    /**
     * 지정된 달에서 byDay 조건에 맞는 가장 빠른 반복 날짜를 계산한다.
     * 요청한 주차가 월 범위를 벗어나면 같은 월의 마지막 해당 요일로 보정한다.
     */
    public LocalDate nextDateInMonth(LocalDate targetMonth) {
        if (isEmpty()) {
            throw new IllegalStateException(RecurrenceRuleError.WEEK_DAYS_EMPTY.message());
        }

        LocalDate firstDayOfMonth = targetMonth.withDayOfMonth(1);
        return days.stream()
                .map(day -> getMonthlyWeekday(firstDayOfMonth, day.toDayOfWeek()))
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }

    /**
     * 현재 월의 byDay 후보 중 기준일 이후의 가장 빠른 반복 날짜를 계산한다.
     */
    public Optional<LocalDate> nextDateAfterInMonth(LocalDate currentDate) {
        if (isEmpty()) {
            throw new IllegalStateException(RecurrenceRuleError.WEEK_DAYS_EMPTY.message());
        }

        LocalDate firstDayOfMonth = currentDate.withDayOfMonth(1);
        return days.stream()
                .map(day -> getMonthlyWeekday(firstDayOfMonth, day.toDayOfWeek()))
                .filter(date -> date.isAfter(currentDate))
                .min(Comparator.naturalOrder());
    }

    public String toRRuleValue() {
        return days.stream()
                .map(day -> offset != 0 ? offset + day.name() : day.name())
                .collect(Collectors.joining(","));
    }

    /**
     * n주차 요일을 계산하고, 없는 5주차 요일은 마지막 같은 요일로 보정한다.
     */
    private LocalDate getMonthlyWeekday(LocalDate firstDayOfMonth, DayOfWeek dayOfWeek) {
        LocalDate candidate = firstDayOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(offset, dayOfWeek));

        if (candidate.getMonth() == firstDayOfMonth.getMonth()) {
            return candidate;
        }

        // 해당 월에 없는 5주차 요일은 마지막 같은 요일로 보정
        return firstDayOfMonth.with(TemporalAdjusters.lastInMonth(dayOfWeek));
    }

}
