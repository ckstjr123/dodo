package com.dodo.todo.todo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TodoHistoryListResponse(
        List<TodoHistoryResponse> histories,
        LocalDateTime nextCursorCompletedAt,
        Long nextCursorId,
        boolean hasNext
) {
}
