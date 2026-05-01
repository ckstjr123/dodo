package com.dodo.todo.recurrencerule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dodo.todo.recurrencerule.RecurrenceRuleError.MONTHLY_BY_DAY_OFFSET_REQUIRED;
import static com.dodo.todo.recurrencerule.RecurrenceRuleError.WEEK_DAYS_EMPTY;

public record WeekDays(
        int offset,
        List<Day> days
) {

    private static final int FIRST_DAY_OF_MONTH = 1;
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
     * 대상 월에서 byDay 조건에 맞는 가장 이른 날짜를 계산함.
     * 예: 2026-02-20, 2MO(두번째 월요일)이면 기준일 이후 여부와 관계없이 2026-02-09를 반환함.
     * 예: 2026-02-01, 5FR이면 5번째 금요일이 없으므로 마지막 금요일인 2026-02-27을 반환함.
     */
    public LocalDate startDateInMonth(LocalDate baseDate) {
        validateNotEmpty();

        LocalDate firstDayOfMonth = baseDate.withDayOfMonth(FIRST_DAY_OF_MONTH);
        return days.stream()
                .map(day -> getMonthlyWeekday(firstDayOfMonth, day.toDayOfWeek()))
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }

    /**
     * 현재 월에서 byDay 조건에 맞고 baseDate 이후인 가장 이른 날짜를 계산함.
     * 예: 2026-02-01, 2MO이면 2026-02-09를 Optional로 반환함.
     * 예: 2026-02-20, 2MO이면 2026-02-09가 이미 지났으므로 Optional.empty()를 반환함.
     */
    public Optional<LocalDate> nextDateInMonthAfter(LocalDate baseDate) {
        validateNotEmpty();

        LocalDate firstDayOfMonth = baseDate.withDayOfMonth(FIRST_DAY_OF_MONTH);
        return days.stream()
                .map(day -> getMonthlyWeekday(firstDayOfMonth, day.toDayOfWeek()))
                .filter(date -> date.isAfter(baseDate))
                .min(Comparator.naturalOrder());
    }

    public String toRRuleValue() {
        return days.stream()
                .map(day -> offset != 0 ? offset + day.name() : day.name())
                .collect(Collectors.joining(","));
    }

    /**
     * n번째 요일을 계산하고, 없는 5번째 요일은 마지막 요일로 보정함.
     */
    private LocalDate getMonthlyWeekday(LocalDate firstDayOfMonth, DayOfWeek dayOfWeek) {
        if (!hasOffset()) {
            throw new IllegalStateException(MONTHLY_BY_DAY_OFFSET_REQUIRED.message());
        }

        LocalDate candidate = firstDayOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(offset, dayOfWeek));

        if (candidate.getMonth() == firstDayOfMonth.getMonth()) {
            return candidate;
        }

        // 대상 월에 없는 5번째 요일은 마지막 요일로 보정
        return firstDayOfMonth.with(TemporalAdjusters.lastInMonth(dayOfWeek));
    }

    private boolean hasOffset() {
        return offset != 0;
    }

    private void validateNotEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException(WEEK_DAYS_EMPTY.message());
        }
    }

}
