package com.dodo.todo.reminder.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderCreateRequestTest {

    @Test
    @DisplayName("생성 요청의 minuteOffset이 null이면 음수 값을 반환한다")
    void returnNegativeMinuteOffsetWhenCreateRequestMinuteOffsetIsNull() {
        ReminderCreateRequest request = new ReminderCreateRequest(null, null, null);

        assertThat(request.minuteOffset()).isNegative();
    }

    @Test
    @DisplayName("수정 요청의 minuteOffset이 null이면 음수 값을 반환한다")
    void returnNegativeMinuteOffsetWhenUpdateRequestMinuteOffsetIsNull() {
        ReminderUpdateRequest request = new ReminderUpdateRequest(null, null);

        assertThat(request.minuteOffset()).isNegative();
    }
}
