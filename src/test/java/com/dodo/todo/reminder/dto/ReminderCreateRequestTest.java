package com.dodo.todo.reminder.dto;

import com.dodo.todo.reminder.domain.ReminderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderCreateRequestTest {

    @Test
    @DisplayName("RELATIVE 타입일 때 minuteOffset이 null이면 유효하지 않다")
    void invalidWhenRelativeTypeAndMinuteOffsetIsNull() {
        ReminderCreateRequest request = new ReminderCreateRequest(1L, ReminderType.RELATIVE, null, null);

        assertThat(request.isValidRelative()).isFalse();
    }

    @Test
    @DisplayName("RELATIVE 타입일 때 minuteOffset이 있으면 유효하다")
    void validWhenRelativeTypeAndMinuteOffsetIsPresent() {
        ReminderCreateRequest request = new ReminderCreateRequest(1L, ReminderType.RELATIVE, 10, null);

        assertThat(request.isValidRelative()).isTrue();
    }

    @Test
    @DisplayName("ABSOLUTE 타입일 때 due가 null이면 유효하지 않다")
    void invalidWhenAbsoluteTypeAndDueIsNull() {
        ReminderCreateRequest request = new ReminderCreateRequest(1L, ReminderType.ABSOLUTE, null, null);

        assertThat(request.isValidAbsolute()).isFalse();
    }

    @Test
    @DisplayName("ABSOLUTE 타입일 때 due가 있으면 유효하다")
    void validWhenAbsoluteTypeAndDueIsPresent() {
        ReminderCreateRequest request = new ReminderCreateRequest(1L, ReminderType.ABSOLUTE, null, java.time.LocalDateTime.now());

        assertThat(request.isValidAbsolute()).isTrue();
    }
}
