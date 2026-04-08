package com.dodo.todo.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.reminder.domain.ReminderType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReminderPolicyTest {

    private final ReminderPolicy reminderPolicy = new ReminderPolicy();

    @Test
    @DisplayName("마감 기준 알림을 Todo에 추가한다")
    void addRelativeReminder() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        TodoCreateRequest.ReminderRequest request = new TodoCreateRequest.ReminderRequest(
                ReminderType.RELATIVE_TO_DUE,
                30,
                null
        );

        reminderPolicy.addReminder(todo, request);

        assertThat(todo.getReminders()).hasSize(1);
        assertThat(todo.getReminders().get(0).getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(todo.getReminders().get(0).getRemindBefore()).isEqualTo(30);
    }

    @Test
    @DisplayName("절대 시각 알림을 Todo에 추가한다")
    void addAbsoluteReminder() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 7, 9, 0);
        TodoCreateRequest.ReminderRequest request = new TodoCreateRequest.ReminderRequest(
                ReminderType.ABSOLUTE_AT,
                null,
                remindAt
        );

        reminderPolicy.addReminder(todo, request);

        assertThat(todo.getReminders()).hasSize(1);
        assertThat(todo.getReminders().get(0).getReminderType()).isEqualTo(ReminderType.ABSOLUTE_AT);
        assertThat(todo.getReminders().get(0).getRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("마감 기준 알림은 Todo 마감 시각이 필요하다")
    void rejectRelativeReminderWithoutDueAt() {
        Todo todo = todo(null);
        TodoCreateRequest.ReminderRequest request = new TodoCreateRequest.ReminderRequest(
                ReminderType.RELATIVE_TO_DUE,
                30,
                null
        );

        assertThatThrownBy(() -> reminderPolicy.addReminder(todo, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Due date is required for relative reminder");
    }

    @Test
    @DisplayName("절대 시각 알림은 알림 시각이 필요하다")
    void rejectAbsoluteReminderWithoutRemindAt() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        TodoCreateRequest.ReminderRequest request = new TodoCreateRequest.ReminderRequest(
                ReminderType.ABSOLUTE_AT,
                null,
                null
        );

        assertThatThrownBy(() -> reminderPolicy.addReminder(todo, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Remind at is required");
    }

    @Test
    @DisplayName("같은 알림 규칙은 중복 추가할 수 없다")
    void rejectDuplicateRelativeReminder() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        TodoCreateRequest.ReminderRequest request = new TodoCreateRequest.ReminderRequest(
                ReminderType.RELATIVE_TO_DUE,
                30,
                null
        );

        reminderPolicy.addReminder(todo, request);

        assertThatThrownBy(() -> reminderPolicy.addReminder(todo, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Duplicate relative reminder");
    }

    @Test
    @DisplayName("같은 절대 시각 알림은 중복 추가할 수 없다")
    void rejectDuplicateAbsoluteReminder() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 7, 9, 0);
        TodoCreateRequest.ReminderRequest request = new TodoCreateRequest.ReminderRequest(
                ReminderType.ABSOLUTE_AT,
                null,
                remindAt
        );

        reminderPolicy.addReminder(todo, request);

        assertThatThrownBy(() -> reminderPolicy.addReminder(todo, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Duplicate absolute reminder");
    }

    private Todo todo(LocalDateTime dueAt) {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");

        return Todo.builder()
                .member(member)
                .category(category)
                .title("문서 정리")
                .status(TodoStatus.OPEN)
                .priority("MEDIUM")
                .dueAt(dueAt)
                .build();
    }
}
