package com.dodo.todo.todo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Todo 목록 응답. 알림 조회와 알림 아이콘 표시용 필드는 후속 과제로 보류")
public record TodoListResponse(
        @ArraySchema(schema = @Schema(description = "Todo 목록. 알림 상세는 포함하지 않음"))
        List<TodoResponse> todos
) {
}
