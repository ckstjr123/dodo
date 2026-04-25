package com.dodo.todo.todo.controller;

import com.dodo.todo.todo.dto.TodoHistoryListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;

@Tag(name = "Todo History", description = "Todo 완료 이력 API")
public interface TodoHistoryApiDocs {

    @Operation(summary = "Todo 완료 이력 조회")
    @SecurityRequirement(name = "bearerAuth")
    TodoHistoryListResponse getHistories(
            Long memberId,
            Long todoId,
            LocalDateTime cursorCompletedAt,
            Long cursorId,
            Integer size
    );
}
