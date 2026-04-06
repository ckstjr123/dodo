package com.dodo.todo.todo.reminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reminder")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "todo_id", nullable = false)
    private Long todoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 20)
    private ReminderType reminderType;

    @Column(name = "remind_before")
    private Integer remindBefore;

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Reminder(
            Long todoId,
            ReminderType reminderType,
            Integer remindBefore,
            LocalDateTime remindAt
    ) {
        validateTodoId(todoId);
        validateReminderFields(reminderType, remindBefore, remindAt);
        this.todoId = todoId;
        this.reminderType = reminderType;
        this.remindBefore = remindBefore;
        this.remindAt = remindAt;
    }

    public static Reminder relativeToDue(Long todoId, int remindBefore) {
        return new Reminder(todoId, ReminderType.RELATIVE_TO_DUE, remindBefore, null);
    }

    public static Reminder absoluteAt(Long todoId, LocalDateTime remindAt) {
        return new Reminder(todoId, ReminderType.ABSOLUTE_AT, null, remindAt);
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

    private void validateTodoId(Long todoId) {
        if (todoId == null) {
            throw new IllegalArgumentException("Todo id is required");
        }
    }

    private void validateReminderFields(
            ReminderType reminderType,
            Integer remindBefore,
            LocalDateTime remindAt
    ) {
        // 마감 기준 알림과 절대 시각 알림은 필요한 값이 다르므로 후보값 기준으로 먼저 검증한다.
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

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
