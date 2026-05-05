package com.dodo.todo.todo.dto;

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

@Schema(description = "Todo 수정 요청")
public record TodoUpdateRequest(
        @Schema(description = "카테고리 ID", example = "1")
        @NotNull
        Long categoryId,

        @Schema(description = "Todo 제목", example = "운동하기", maxLength = 200)
        @NotBlank
        @Size(max = 200)
        String title,

        @Schema(description = "메모", example = "퇴근 후 헬스장 가기", maxLength = 1000, nullable = true)
        @Size(max = 1000)
        String memo,

        @Schema(description = "정렬 순서", example = "0", nullable = true)
        Integer sortOrder,

        @Schema(description = "마감 일시. UTC Z suffix 없는 ISO-8601 LocalDateTime 형식", example = "2026-05-03T19:48:47", type = "string", format = "date-time", nullable = true)
        LocalDateTime dueAt,

        @Schema(description = "예약 날짜. 오늘 또는 미래 날짜", example = "2026-05-03", type = "string", format = "date", nullable = true)
        @FutureOrPresent
        LocalDate scheduledDate,

        @Schema(description = "예약 시간. HH:mm:ss 형식", example = "19:48:00", type = "string", format = "time", nullable = true)
        LocalTime scheduledTime,

        @Schema(description = "반복 설정. 반복이 없으면 null", implementation = TodoRecurrenceRequest.class, nullable = true)
        @Valid
        TodoRecurrenceRequest recurrence
) {

    public TodoRecurrence getRecurrence() {
        if (recurrence == null) {
            return null;
        }

        return recurrence.toTodoRecurrence();
    }
}
