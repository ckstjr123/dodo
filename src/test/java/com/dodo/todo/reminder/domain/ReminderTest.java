package com.dodo.todo.reminder.domain;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
import com.dodo.todo.todo.domain.Todo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.dodo.todo.util.TestFixture.createScheduledTodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderTest {

    @Test
    @DisplayName("상대 알림은 minuteOffset 설정을 저장한다")
    void createRelativeReminder() {
        Member member = member();
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));

        RelativeReminder reminder = RelativeReminder.create(todo, member, 30);

        assertThat(reminder.getType()).isEqualTo(ReminderType.RELATIVE);
        assertThat(reminder.getMinuteOffset()).isEqualTo(30);
        assertThat(reminder.calculateRemindAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 8, 30));
    }

    @Test
    @DisplayName("절대 알림은 due 설정을 저장한다")
    void createAbsoluteReminder() {
        Member member = member();
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        LocalDateTime due = LocalDateTime.of(2026, 5, 19, 8, 0);

        AbsoluteReminder reminder = AbsoluteReminder.create(todo, member, due);

        assertThat(reminder.getType()).isEqualTo(ReminderType.ABSOLUTE);
        assertThat(reminder.getDue()).isEqualTo(due);
        assertThat(reminder.calculateRemindAt()).isEqualTo(due);
    }

    @Test
    @DisplayName("due 없이 절대 알림을 생성할 수 없다")
    void rejectCreateAbsoluteReminderWithoutDue() {
        Member member = member();
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));

        assertThatThrownBy(() -> AbsoluteReminder.create(todo, member, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_DUE_REQUIRED.message());
    }

    @Test
    @DisplayName("일정이 없는 Todo에 알림을 생성할 수 없다")
    void rejectReminderWithoutTodoSchedule() {
        Member member = member();
        Todo todo = Todo.builder()
                .member(member)
                .title("title")
                .build();
        LocalDateTime due = LocalDateTime.of(2026, 5, 19, 8, 0);

        assertThatThrownBy(() -> AbsoluteReminder.create(todo, member, due))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_SCHEDULE_REQUIRED.message());

        assertThatThrownBy(() -> RelativeReminder.create(todo, member, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_SCHEDULE_REQUIRED.message());
    }

    @Test
    @DisplayName("음수 minuteOffset으로 상대 알림을 생성할 수 없다")
    void rejectNegativeMinuteOffset() {
        Member member = member();
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));

        assertThatThrownBy(() -> RelativeReminder.create(todo, member, -1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReminderError.REMINDER_OFFSET_NEGATIVE.message());
    }

    @Test
    @DisplayName("상대 알림 수정은 타입을 유지하고 minuteOffset만 변경한다")
    void updateRelativeReminder() {
        Member member = member();
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        RelativeReminder reminder = RelativeReminder.create(todo, member, 10);

        reminder.update(new ReminderUpdateRequest(30, null));

        assertThat(reminder.getType()).isEqualTo(ReminderType.RELATIVE);
        assertThat(reminder.getMinuteOffset()).isEqualTo(30);
    }

    @Test
    @DisplayName("절대 알림 수정은 타입을 유지하고 due만 변경한다")
    void updateAbsoluteReminder() {
        Member member = member();
        Todo todo = createScheduledTodo(member, "work", "todo", LocalDate.of(2026, 5, 20), LocalTime.of(9, 0));
        AbsoluteReminder reminder = AbsoluteReminder.create(todo, member, LocalDateTime.of(2026, 5, 19, 8, 0));
        LocalDateTime changedDue = LocalDateTime.of(2026, 5, 21, 8, 0);

        reminder.update(new ReminderUpdateRequest(null, changedDue));

        assertThat(reminder.getType()).isEqualTo(ReminderType.ABSOLUTE);
        assertThat(reminder.getDue()).isEqualTo(changedDue);
    }

    private Member member() {
        return Member.from("member@example.com");
    }

}
