package com.dodo.todo.todo.dto;

import com.dodo.todo.reminder.dto.ReminderRequest;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Todo 생성 요청")
public record TodoRequest(
        @Schema(description = "카테고리 ID", example = "1")
        @NotNull
        Long categoryId,

        @Schema(description = "상위 Todo ID. 최상위 Todo이면 null", example = "null", nullable = true)
        Long parentTodoId,

        @Schema(description = "Todo 제목", example = "운동하기", maxLength = 200)
        @NotBlank
        @Size(max = 200)
        String title,

        @Schema(description = "메모", example = "퇴근 후 헬스장 가기", maxLength = 1000, nullable = true)
        @Size(max = 1000)
        String memo,

        @Schema(description = "정렬 순서", example = "0", nullable = true)
        Integer sortOrder,

        @Schema(description = "마감 일시. UTC Z suffix 없이 ISO-8601 LocalDateTime 형식", example = "2026-05-03T19:48:47", type = "string", format = "date-time", nullable = true)
        LocalDateTime dueAt,

        @Schema(description = "예정 날짜. 오늘 또는 미래 날짜", example = "2026-05-03", type = "string", format = "date", nullable = true)
        @FutureOrPresent
        LocalDate scheduledDate,

        @Schema(description = "예정 시간. HH:mm:ss 형식", example = "19:48:00", type = "string", format = "time", nullable = true)
        LocalTime scheduledTime,

        @Schema(description = "반복 설정. 반복이 없으면 null", implementation = TodoRecurrenceRequest.class, nullable = true)
        @Valid
        TodoRecurrenceRequest recurrence,

        @Schema(description = "초기 미리 알림 목록. 생성 후 알림 관리는 Reminder API를 사용", nullable = true)
        @Valid
        List<ReminderRequest> reminders
) {

    public TodoRequest(
            Long categoryId,
            Long parentTodoId,
            String title,
            String memo,
            Integer sortOrder,
            LocalDateTime dueAt,
            LocalDate scheduledDate,
            LocalTime scheduledTime,
            TodoRecurrenceRequest recurrence
    ) {
        this(
                categoryId,
                parentTodoId,
                title,
                memo,
                sortOrder,
                dueAt,
                scheduledDate,
                scheduledTime,
                recurrence,
                null
        );
    }

    public TodoRecurrence getRecurrence() {
        if (recurrence == null) {
            return null;
        }

        return recurrence.toTodoRecurrence();
    }

}
