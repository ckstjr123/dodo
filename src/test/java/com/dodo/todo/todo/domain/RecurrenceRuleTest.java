package com.dodo.todo.todo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.domain.recurrence.WeekDays;
import java.time.LocalDate;
import java.util.List;
import net.fortuna.ical4j.model.WeekDay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecurrenceRuleTest {

    @Test
    @DisplayName("일 반복은 ical4j를 사용해 다음 날짜를 계산한다")
    void dailyNextDate() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 2, WeekDays.empty(), null, null);

        assertThat(rule.nextDate(LocalDate.of(2026, 4, 7))).isEqualTo(LocalDate.of(2026, 4, 9));
    }

    @Test
    @DisplayName("주 반복은 RFC 5545 BYDAY를 사용해 다음 날짜를 계산한다")
    void weeklyNextDate() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.WEEKLY, 1, WeekDays.from(List.of(WeekDay.MO, WeekDay.FR)), null, null);

        assertThat(rule.nextDate(LocalDate.of(2026, 4, 7))).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    @DisplayName("월 특정일 반복은 해당 월 마지막 날로 보정한다")
    void monthlyByMonthDayAdjustsLastDay() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY, 1, WeekDays.empty(), 31, null);

        assertThat(rule.nextDate(LocalDate.of(2026, 1, 31))).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("월 5주차 요일 반복은 마지막 해당 요일로 보정한다")
    void monthlyFifthWeekAdjustsLastWeekday() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY, 1, WeekDays.from(List.of(new WeekDay(WeekDay.FR, 5))), null, null);

        assertThat(rule.nextDate(LocalDate.of(2026, 1, 30))).isEqualTo(LocalDate.of(2026, 2, 27));
    }

    @Test
    @DisplayName("주 반복은 RFC 5545 요일 값만 허용한다")
    void rejectInvalidWeeklyByDay() {
        assertThatThrownBy(() -> new RecurrenceRule(Frequency.WEEKLY, 1, WeekDays.from(List.of(new WeekDay(WeekDay.MO, 1))), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weekly recurrence must not have offsets");
    }

    @Test
    @DisplayName("월 반복도 종료일을 넘으면 null을 반환한다")
    void monthlyNextDateReturnsNullAfterUntil() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY, 1, WeekDays.empty(), 31, LocalDate.of(2026, 2, 28));

        assertThat(rule.nextDate(LocalDate.of(2026, 2, 28))).isNull();
    }

    @Test
    @DisplayName("UNTIL 값을 RRULE 문자열에 포함한다")
    void includeUntilInRRuleText() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, LocalDate.of(2026, 4, 10));

        assertThat(rule.toRRuleText()).isEqualTo("FREQ=DAILY;INTERVAL=1;UNTIL=20260410");
    }
}
