package com.dodo.todo.todo.dto;

import com.dodo.todo.reminder.dto.ReminderResponse;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Todo 상세 응답. 현재 Todo의 알림 상세를 포함")
public record TodoDetailResponse(
        @Schema(description = "Todo ID", example = "1")
        Long todoId,

        @Schema(description = "상위 Todo ID. 최상위 Todo이면 null", example = "null", nullable = true)
        Long parentTodoId,

        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,

        @Schema(description = "카테고리 이름", example = "개인")
        String categoryName,

        @Schema(description = "Todo 제목", example = "운동하기")
        String title,

        @Schema(description = "메모", nullable = true)
        String memo,

        @Schema(description = "Todo 상태", example = "TODO", allowableValues = {"TODO", "DONE"})
        TodoStatus status,

        @Schema(description = "정렬 순서", example = "0")
        int sortOrder,

        @Schema(description = "마감 일시", type = "string", format = "date-time", nullable = true)
        LocalDateTime dueAt,

        @Schema(description = "예정 날짜", type = "string", format = "date", nullable = true)
        LocalDate scheduledDate,

        @Schema(description = "예정 시간", type = "string", format = "time", nullable = true)
        LocalTime scheduledTime,

        @Schema(description = "반복 설정", nullable = true)
        TodoRecurrenceResponse recurrence,

        @ArraySchema(schema = @Schema(description = "현재 Todo에 설정된 미리 알림"))
        List<ReminderResponse> reminders,

        @ArraySchema(schema = @Schema(description = "하위 Todo 목록 항목. 하위 Todo 알림 상세는 포함하지 않음"))
        List<TodoResponse> subTodos
) {

    public static TodoDetailResponse from(Todo todo) {
        return new TodoDetailResponse(
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
                todo.getReminders().stream()
                        .map(ReminderResponse::from)
                        .toList(),
                todo.getSubTodos().stream()
                        .map(TodoResponse::from)
                        .toList()
        );
    }
}
