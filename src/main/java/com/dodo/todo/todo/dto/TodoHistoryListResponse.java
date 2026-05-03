package com.dodo.todo.todo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "완료 이력 목록 응답")
public record TodoHistoryListResponse(
        @ArraySchema(schema = @Schema(description = "완료 이력"))
        List<TodoHistoryResponse> histories,

        @Schema(description = "다음 페이지 커서 완료 일시. 다음 페이지가 없으면 null", example = "2026-05-03T19:48:47", type = "string", format = "date-time", nullable = true)
        LocalDateTime nextCursorCompletedAt,

        @Schema(description = "다음 페이지 커서 ID. 다음 페이지가 없으면 null", example = "10", nullable = true)
        Long nextCursorId,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
