package com.dodo.todo.reminder.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.dodo.todo.util.TestFixture.createCategory;
import static com.dodo.todo.util.TestFixture.createMember;
import static com.dodo.todo.util.TestFixture.createScheduledTodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderTest {

    @Test
    @DisplayName("minuteOffset 기준으로 알림 시각을 계산한다")
    void calculateRemindAtByMinuteOffset() {
        Member member = createMember(1L);
        Category category = createCategory(member, "work");
        Todo todo = createScheduledTodo(10L, member, category, "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));

        Reminder reminder = Reminder.create(todo, member, 30);

        assertThat(reminder.getRemindAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 8, 30));
    }

    @Test
    @DisplayName("계산된 알림 시각이 과거 날짜여도 생성할 수 있다")
    void allowPastRemindAt() {
        Member member = createMember(1L);
        Category category = createCategory(member, "work");
        Todo todo = createScheduledTodo(10L, member, category, "todo", LocalDate.of(2026, 5, 1), LocalTime.of(0, 10));

        Reminder reminder = Reminder.create(todo, member, 30);

        assertThat(reminder.getRemindAt()).isEqualTo(LocalDateTime.of(2026, 4, 30, 23, 40));
    }

    @Test
    @DisplayName("음수 minuteOffset으로 알림을 생성할 수 없다")
    void rejectNegativeMinuteOffset() {
        Member member = createMember(1L);
        Category category = createCategory(member, "work");
        Todo todo = createScheduledTodo(10L, member, category, "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));

        assertThatThrownBy(() -> Reminder.create(todo, member, -1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_OFFSET_NEGATIVE.message());
    }

    @Test
    @DisplayName("minuteOffset 수정 시 알림 시각을 재계산한다")
    void updateMinuteOffsetRecalculatesRemindAt() {
        Member member = createMember(1L);
        Category category = createCategory(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 5, 20);
        LocalTime scheduledTime = LocalTime.of(9, 0);
        int changedMinuteOffset = 60;
        Todo todo = createScheduledTodo(10L, member, category, "todo", scheduledDate, scheduledTime);
        Reminder reminder = Reminder.create(todo, member, 10);

        reminder.updateMinuteOffset(changedMinuteOffset);

        assertThat(reminder.getMinuteOffset()).isEqualTo(changedMinuteOffset);
        assertThat(reminder.getRemindAt())
                .isEqualTo(LocalDateTime.of(scheduledDate, scheduledTime).minusMinutes(changedMinuteOffset));
    }

    @Test
    @DisplayName("연관된 Todo 날짜를 통해 알림 시각을 재계산한다")
    void rescheduleUsesAssociatedTodoSchedule() {
        Member member = createMember(1L);
        Category category = createCategory(member, "work");
        Todo todo = createScheduledTodo(10L, member, category, "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        int minuteOffset = 15;
        LocalDate changedDate = LocalDate.of(2026, 5, 21);
        LocalTime changedTime = LocalTime.of(10, 0);
        Reminder reminder = Reminder.create(todo, member, minuteOffset);

        todo.updateDetails(
                category,
                "updated",
                null,
                null,
                null,
                changedDate,
                changedTime,
                null
        );
        reminder.reschedule();

        assertThat(reminder.getRemindAt()).isEqualTo(LocalDateTime.of(changedDate, changedTime).minusMinutes(minuteOffset));
    }
}
