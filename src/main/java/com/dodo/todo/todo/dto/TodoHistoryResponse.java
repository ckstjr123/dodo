package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.TodoHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "완료 이력 응답")
public record TodoHistoryResponse(
        @Schema(description = "완료 이력 ID", example = "1")
        Long historyId,

        @Schema(description = "할 일 ID", example = "1")
        Long todoId,

        @Schema(description = "완료 당시 할 일 제목", example = "운동하기")
        String title,

        @Schema(description = "완료 일시", example = "2026-05-03T19:48:47", type = "string", format = "date-time")
        LocalDateTime completedAt
) {

    public static TodoHistoryResponse from(TodoHistory history) {
        return new TodoHistoryResponse(
                history.getId(),
                history.getTodoId(),
                history.getTitle(),
                history.getCompletedAt()
        );
    }
}
