package com.dodo.todo.todo.reminder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReminderTest {

    @Test
    @DisplayName("마감 기준 알림은 분 단위의 remindBefore로 생성한다")
    void createRelativeReminder() {
        Reminder reminder = Reminder.relativeToDue(1L, 5);

        assertThat(reminder.getTodoId()).isEqualTo(1L);
        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(reminder.getRemindBefore()).isEqualTo(5);
        assertThat(reminder.getRemindAt()).isNull();
    }

    @Test
    @DisplayName("절대 시각 알림은 remindAt으로 생성한다")
    void createAbsoluteReminder() {
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 6, 9, 0);
        Reminder reminder = Reminder.absoluteAt(1L, remindAt);

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.ABSOLUTE_AT);
        assertThat(reminder.getRemindBefore()).isNull();
        assertThat(reminder.getRemindAt()).isEqualTo(remindAt);
    }

    @Test
    @DisplayName("마감 기준 알림의 분 값은 1 이상이어야 한다")
    void rejectInvalidRemindBefore() {
        assertThatThrownBy(() -> Reminder.relativeToDue(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relative reminder requires remindBefore of at least 1 minute");
    }

    @Test
    @DisplayName("절대 시각 알림은 remindAt이 필요하다")
    void rejectAbsoluteReminderWithoutRemindAt() {
        assertThatThrownBy(() -> Reminder.absoluteAt(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Absolute reminder requires remindAt");
    }

    @Test
    @DisplayName("알림 유형을 마감 기준으로 변경하면 절대 시각 값은 비운다")
    void updateReminderToRelativeType() {
        Reminder reminder = Reminder.absoluteAt(1L, LocalDateTime.of(2026, 4, 6, 9, 0));

        reminder.updateRelativeToDue(60);

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(reminder.getRemindBefore()).isEqualTo(60);
        assertThat(reminder.getRemindAt()).isNull();
    }

    @Test
    @DisplayName("알림 유형을 절대 시각으로 변경하면 상대 알림 값은 비운다")
    void updateReminderToAbsoluteType() {
        Reminder reminder = Reminder.relativeToDue(1L, 10);
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 7, 8, 30);

        reminder.updateAbsoluteAt(remindAt);

        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.ABSOLUTE_AT);
        assertThat(reminder.getRemindBefore()).isNull();
        assertThat(reminder.getRemindAt()).isEqualTo(remindAt);
    }

}
