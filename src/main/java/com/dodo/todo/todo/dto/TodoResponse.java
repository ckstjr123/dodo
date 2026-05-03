package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "할 일 응답")
public record TodoResponse(
        @Schema(description = "할 일 ID", example = "1")
        Long todoId,

        @Schema(description = "상위 할 일 ID. 최상위 할 일이면 null", example = "null", nullable = true)
        Long parentTodoId,

        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,

        @Schema(description = "카테고리 이름", example = "개인")
        String categoryName,

        @Schema(description = "할 일 제목", example = "운동하기")
        String title,

        @Schema(description = "메모", example = "퇴근 후 헬스장 가기", nullable = true)
        String memo,

        @Schema(description = "할 일 상태", example = "TODO", allowableValues = {"TODO", "DONE"})
        TodoStatus status,

        @Schema(description = "정렬 순서", example = "0")
        int sortOrder,

        @Schema(description = "마감 일시", example = "2026-05-03T19:48:47", type = "string", format = "date-time", nullable = true)
        LocalDateTime dueAt,

        @Schema(description = "예약 날짜", example = "2026-05-03", type = "string", format = "date", nullable = true)
        LocalDate scheduledDate,

        @Schema(description = "예약 시간", example = "19:48:00", type = "string", format = "time", nullable = true)
        LocalTime scheduledTime,

        @Schema(description = "반복 설정", nullable = true)
        TodoRecurrenceResponse recurrence,

        @ArraySchema(schema = @Schema(description = "하위 할 일"))
        List<TodoResponse> subTodos
) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getMainTodoId(),
                todo.getCategoryId(),
                todo.getCategory().getName(),
                todo.getTitle(),
                todo.getMemo(),
                todo.getStatus(),
                todo.getSortOrder(),
                todo.getDueAt(),
                todo.getScheduledDate(),
                todo.getScheduledTime(),
                TodoRecurrenceResponse.from(todo.getRecurrence()),
                todo.getSubTodos().stream()
                        .map(TodoResponse::from)
                        .toList()
        );
    }
}
