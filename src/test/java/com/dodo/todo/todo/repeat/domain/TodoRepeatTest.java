package com.dodo.todo.todo.repeat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TodoRepeatTest {

    @Test
    @DisplayName("일간 반복은 반복 간격만으로 생성한다")
    void createDailyRepeat() {
        TodoRepeat todoRepeat = TodoRepeat.daily(1L, 3);

        assertThat(todoRepeat.getTodoId()).isEqualTo(1L);
        assertThat(todoRepeat.getRepeatType()).isEqualTo(TodoRepeatType.DAILY);
        assertThat(todoRepeat.getRepeatInterval()).isEqualTo(3);
        assertThat(todoRepeat.getDaysOfWeek()).isEmpty();
    }

    @Test
    @DisplayName("주간 반복은 주 간격과 요일 목록으로 생성한다")
    void createWeeklyRepeat() {
        TodoRepeat todoRepeat = TodoRepeat.weekly(
                1L,
                2,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        );

        assertThat(todoRepeat.getRepeatType()).isEqualTo(TodoRepeatType.WEEKLY);
        assertThat(todoRepeat.getRepeatInterval()).isEqualTo(2);
        assertThat(todoRepeat.getDaysOfWeek()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
    }

    @Test
    @DisplayName("반복 간격은 1 이상이어야 한다")
    void rejectInvalidRepeatInterval() {
        assertThatThrownBy(() -> TodoRepeat.daily(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Repeat interval must be at least 1");
    }

    @Test
    @DisplayName("주간 반복은 최소 한 개 이상의 요일이 필요하다")
    void rejectWeeklyRepeatWithoutDays() {
        assertThatThrownBy(() -> TodoRepeat.weekly(1L, 1, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weekly repeat requires at least one day of week");
    }

    @Test
    @DisplayName("일간 반복에는 요일 목록을 설정할 수 없다")
    void rejectDailyRepeatWithDaysOfWeek() {
        TodoRepeat todoRepeat = TodoRepeat.daily(1L, 1);

        assertThatThrownBy(() -> todoRepeat.updateDaysOfWeek(Set.of(DayOfWeek.MONDAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Daily repeat cannot have days of week");
    }

    @Test
    @DisplayName("주간 반복 요일은 중복 없이 정렬해 저장한다")
    void updateDaysOfWeekDeduplicatesAndSortsDays() {
        TodoRepeat todoRepeat = TodoRepeat.weekly(
                1L,
                1,
                Set.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
        );

        todoRepeat.updateDaysOfWeek(Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY));

        assertThat(todoRepeat.getDaysOfWeek()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    }

    @Test
    @DisplayName("요일 목록은 JSON 배열로 변환한다")
    void convertDaysOfWeekToJson() {
        DayOfWeekSetJsonConverter converter = new DayOfWeekSetJsonConverter();

        String json = converter.convertToDatabaseColumn(Set.of(DayOfWeek.THURSDAY, DayOfWeek.MONDAY));

        assertThat(json).isEqualTo("[\"MONDAY\",\"THURSDAY\"]");
    }

    @Test
    @DisplayName("JSON 배열은 요일 집합으로 복원한다")
    void convertJsonToDaysOfWeek() {
        DayOfWeekSetJsonConverter converter = new DayOfWeekSetJsonConverter();

        Set<DayOfWeek> daysOfWeek = converter.convertToEntityAttribute("[\"MONDAY\",\"THURSDAY\"]");

        assertThat(daysOfWeek).containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
    }
}
