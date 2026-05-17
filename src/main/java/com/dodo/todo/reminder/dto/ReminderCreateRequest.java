package com.dodo.todo.reminder.dto;

import com.dodo.todo.reminder.domain.ReminderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

import static com.dodo.todo.reminder.dto.ReminderRequestUtil.resolveMinuteOffset;

@Schema(description = "미리 알림 생성 요청")
public record ReminderCreateRequest(
        @Schema(description = "알림 타입. 생략하면 RELATIVE로 처리", example = "RELATIVE", nullable = true)
        ReminderType type,

        @Schema(description = "일정 시각 기준 몇 분 전에 알림을 보낼지", example = "10", nullable = true)
        @PositiveOrZero
        Integer minuteOffset,

        @Schema(description = "절대 알림 일시", example = "2026-05-20T09:00:00", nullable = true)
        LocalDateTime due
) {

    @Override
    public Integer minuteOffset() {
        return resolveMinuteOffset(minuteOffset);
    }

    /**
     * 생성 알림 타입 조회
     * 타입을 생략한 기존 요청은 상대 알림으로 처리함.
     */
    public ReminderType getReminderType() {
        return type == null ? ReminderType.RELATIVE : type;
    }
}
