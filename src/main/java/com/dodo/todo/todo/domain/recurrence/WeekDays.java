package com.dodo.todo.todo.domain.recurrence;

import net.fortuna.ical4j.model.WeekDay;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class WeekDays {

    private static final WeekDays EMPTY = new WeekDays(List.of());

    private final List<WeekDay> weekDays;

    private WeekDays(List<WeekDay> weekDays) {
        this.weekDays = Collections.unmodifiableList(weekDays);
    }

    public static WeekDays empty() {
        return EMPTY;
    }

    public static WeekDays from(List<WeekDay> weekDays) {
        if (weekDays == null || weekDays.isEmpty()) {
            return empty();
        }

        return new WeekDays(weekDays);
    }

    public boolean isEmpty() {
        return weekDays.isEmpty();
    }

    public Optional<LocalDate> findEarliestDateAfter(LocalDate baseDate, Function<WeekDay, LocalDate> dateProvider) {
        return weekDays.stream()
                .map(dateProvider)
                .filter(date -> date.isAfter(baseDate))
                .min(LocalDate::compareTo);
    }

    public Optional<LocalDate> findEarliestDate(Function<WeekDay, LocalDate> dateProvider) {
        return weekDays.stream()
                .map(dateProvider)
                .min(LocalDate::compareTo);
    }

    public boolean hasOrdinalWeekday() {
        return weekDays.stream().anyMatch(day -> day.getOffset() != 0);
    }

    public boolean hasOffsetOutOfRange(int minOffset, int maxOffset) {
        return weekDays.stream().anyMatch(day -> day.getOffset() < minOffset || day.getOffset() > maxOffset);
    }

    public String toRRuleValue() {
        return String.join(",", toStrings());
    }

    public List<String> toStrings() {
        return weekDays.stream()
                .map(WeekDay::toString)
                .toList();
    }

}
