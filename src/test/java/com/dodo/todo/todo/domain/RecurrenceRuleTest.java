package com.dodo.todo.todo.domain;

import com.dodo.todo.todo.domain.recurrence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceRuleTest {

    @Test
    @DisplayName("일 반복은 ical4j를 사용해 다음 날짜를 계산한다")
    void dailyNextDate() {
        int interval = 2;
        LocalDate date = LocalDate.of(2026, 4, 7);
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.DAILY,
                interval,
                WeekDays.empty(),
                null,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.nextDate(date))
                .contains(date.plusDays(interval));
    }

    @Test
    @DisplayName("주 반복은 RFC 5545 BYDAY를 사용해 다음 날짜를 계산한다")
    void weeklyNextDate() {
        LocalDate date = LocalDate.of(2026, 4, 7);
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.WEEKLY,
                1,
                WeekDays.of(0, List.of(Day.MO, Day.FR)),
                null,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.nextDate(date))
                .contains(date.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)));
    }

    @Test
    @DisplayName("월 n주차 요일 반복은 다음 달의 지정 주차 요일을 계산한다")
    void monthlyNthWeekdayNextDate() {
        LocalDate date = LocalDate.of(2026, 1, 12);
        LocalDate nextMonth = date.withDayOfMonth(1).plusMonths(1);
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.MONTHLY,
                1,
                WeekDays.of(2, List.of(Day.MO)),
                null,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.nextDate(date))
                .contains(nextMonth.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY)));
    }

    @Test
    @DisplayName("월 byDay일 때 같은 달에 남은 후보가 있으면 다음 달로 넘어가지 않는다")
    void monthlyByDaySelectsSameMonthWeekdayAfterCurrentDate() {
        LocalDate date = LocalDate.of(2026, 2, 1);
        LocalDate month = date.withDayOfMonth(1);
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.MONTHLY,
                1,
                WeekDays.of(2, List.of(Day.MO)),
                null,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.nextDate(date))
                .contains(month.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY)));
    }

    @Test
    @DisplayName("5주차가 없는 달은 마지막 주 요일로 보정한다")
    void monthlyFifthWeekAdjustsLastWeekday() {
        LocalDate date = LocalDate.of(2026, 1, 30);
        LocalDate nextMonth = date.withDayOfMonth(1).plusMonths(1);
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.MONTHLY,
                1,
                WeekDays.of(5, List.of(Day.FR)),
                null,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.nextDate(date))
                .contains(nextMonth.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY)));
    }

    @Test
    @DisplayName("월 반복 일자가 대상 월 마지막 날보다 크면 말일로 보정한다")
    void monthlyByMonthDayAdjustsLastDay() {
        LocalDate date = LocalDate.of(2026, 1, 31);
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.MONTHLY,
                1,
                WeekDays.empty(),
                31,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.nextDate(date))
                .contains(YearMonth.from(date).plusMonths(1).atEndOfMonth());
    }

    @Test
    @DisplayName("요청 계층 검증을 거치지 않으면 생성자는 byDay와 byMonthDay를 함께 받을 수 있다")
    void constructorAcceptsByDayAndByMonthDay() {
        RecurrenceRule rule = new RecurrenceRule(
                Frequency.MONTHLY,
                1,
                WeekDays.of(2, List.of(Day.MO)),
                15,
                null,
                RecurrenceCriteria.SCHEDULED_DATE
        );

        assertThat(rule.byDay().days()).containsExactly(Day.MO);
        assertThat(rule.byMonthDay()).isEqualTo(15);
    }

}
