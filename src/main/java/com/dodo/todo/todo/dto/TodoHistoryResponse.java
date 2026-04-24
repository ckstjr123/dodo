package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.TodoHistory;
import java.time.LocalDateTime;

public record TodoHistoryResponse(
        Long historyId,
        Long todoId,
        String title,
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
