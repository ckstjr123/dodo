package com.dodo.todo.todo.reminder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReminderTest {

    @Test
    @DisplayName("마감 기준 알림은 Todo와 remindBefore로 생성한다")
    void createRelativeReminder() {
        Todo todo = todo();
        Reminder reminder = Reminder.relativeToDue(todo, 5);

        assertThat(reminder.getTodo()).isSameAs(todo);
        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(reminder.getRemindBefore()).isEqualTo(5);
        assertThat(reminder.getRemindAt()).isNull();
    }

    @Test
    @DisplayName("절대 시각 알림은 remindAt으로 생성한다")
    void createAbsoluteReminder() {
        Todo todo = todo();
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 6, 9, 0);
        Reminder reminder = Reminder.absoluteAt(todo, remindAt);

        assertThat(reminder.getTodo()).isSameAs(todo);
        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.ABSOLUTE_AT);
        assertThat(reminder.getRemindBefore()).isNull();
        assertThat(reminder.getRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("마감 기준 알림의 분 값은 1 이상이어야 한다")
    void rejectInvalidRemindBefore() {
        assertThatThrownBy(() -> Reminder.relativeToDue(todo(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relative reminder requires remindBefore of at least 1 minute");
    }

    @Test
    @DisplayName("절대 시각 알림은 remindAt이 필요하다")
    void rejectAbsoluteReminderWithoutRemindAt() {
        assertThatThrownBy(() -> Reminder.absoluteAt(todo(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Absolute reminder requires remindAt");
    }

    @Test
    @DisplayName("알림 타입을 마감 기준으로 바꾸면 절대 시각 값이 비워진다")
    void updateReminderToRelativeType() {
        Reminder reminder = Reminder.absoluteAt(todo(), LocalDateTime.of(2026, 4, 6, 9, 0));

        reminder.updateRelativeToDue(60);

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(reminder.getRemindBefore()).isEqualTo(60);
        assertThat(reminder.getRemindAt()).isNull();
    }

    @Test
    @DisplayName("알림 타입을 절대 시각으로 바꾸면 상대 알림 값이 비워진다")
    void updateReminderToAbsoluteType() {
        Reminder reminder = Reminder.relativeToDue(todo(), 10);
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 7, 8, 30);

        reminder.updateAbsoluteAt(remindAt);

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.ABSOLUTE_AT);
        assertThat(reminder.getRemindBefore()).isNull();
        assertThat(reminder.getRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("잘못된 마감 기준 알림으로 수정하면 기존 상태를 유지한다")
    void keepPreviousStateWhenUpdateRelativeToDueFails() {
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 6, 9, 0);
        Reminder reminder = Reminder.absoluteAt(todo(), remindAt);

        assertThatThrownBy(() -> reminder.updateRelativeToDue(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relative reminder requires remindBefore of at least 1 minute");

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.ABSOLUTE_AT);
        assertThat(reminder.getRemindBefore()).isNull();
        assertThat(reminder.getRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("잘못된 절대 시각 알림으로 수정하면 기존 상태를 유지한다")
    void keepPreviousStateWhenUpdateAbsoluteAtFails() {
        Reminder reminder = Reminder.relativeToDue(todo(), 10);

        assertThatThrownBy(() -> reminder.updateAbsoluteAt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Absolute reminder requires remindAt");

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(reminder.getRemindBefore()).isEqualTo(10);
        assertThat(reminder.getRemindAt()).isNull();
    }

    private Todo todo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");

        return Todo.builder()
                .member(member)
                .category(category)
                .title("문서 정리")
                .status(TodoStatus.OPEN)
                .priority("MEDIUM")
                .build();
    }
}
