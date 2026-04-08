package com.dodo.todo.todo.reminder.domain;

import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.todo.domain.Todo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reminder")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 20)
    private ReminderType reminderType;

    @Column(name = "remind_before")
    private Integer remindBefore;

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    private Reminder(
            Todo todo,
            ReminderType reminderType,
            Integer remindBefore,
            LocalDateTime remindAt
    ) {
        validateTodo(todo);
        validateReminderFields(reminderType, remindBefore, remindAt);
        this.todo = todo;
        this.reminderType = reminderType;
        this.remindBefore = remindBefore;
        this.remindAt = remindAt;
    }

    public static Reminder relativeToDue(Todo todo, int remindBefore) {
        return new Reminder(todo, ReminderType.RELATIVE_TO_DUE, remindBefore, null);
    }

    public static Reminder absoluteAt(Todo todo, LocalDateTime remindAt) {
        return new Reminder(todo, ReminderType.ABSOLUTE_AT, null, remindAt);
    }

    public Long getTodoId() {
        return todo.getId();
    }

    public boolean isRelativeToDue(int remindBefore) {
        return reminderType == ReminderType.RELATIVE_TO_DUE
                && Objects.equals(this.remindBefore, remindBefore);
    }

    public boolean isAbsoluteAt(LocalDateTime remindAt) {
        return reminderType == ReminderType.ABSOLUTE_AT
                && Objects.equals(this.remindAt, remindAt);
    }

    public void updateRelativeToDue(int remindBefore) {
        validateReminderFields(ReminderType.RELATIVE_TO_DUE, remindBefore, null);
        this.reminderType = ReminderType.RELATIVE_TO_DUE;
        this.remindBefore = remindBefore;
        this.remindAt = null;
    }

    public void updateAbsoluteAt(LocalDateTime remindAt) {
        validateReminderFields(ReminderType.ABSOLUTE_AT, null, remindAt);
        this.reminderType = ReminderType.ABSOLUTE_AT;
        this.remindBefore = null;
        this.remindAt = remindAt;
    }

    private void validateTodo(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo is required");
        }
    }

    private void validateReminderFields(
            ReminderType reminderType,
            Integer remindBefore,
            LocalDateTime remindAt
    ) {
        // 마감 기준 알림과 절대 시각 알림은 필요한 값이 다르므로 타입 기준으로 먼저 검증한다.
        if (reminderType == ReminderType.RELATIVE_TO_DUE) {
            if (remindBefore == null || remindBefore < 1) {
                throw new IllegalArgumentException("Relative reminder requires remindBefore of at least 1 minute");
            }
            return;
        }

        if (reminderType == ReminderType.ABSOLUTE_AT && remindAt == null) {
            throw new IllegalArgumentException("Absolute reminder requires remindAt");
        }
    }
}
