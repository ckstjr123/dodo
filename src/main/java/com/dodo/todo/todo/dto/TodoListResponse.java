package com.dodo.todo.todo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "할 일 목록 응답")
public record TodoListResponse(
        @ArraySchema(schema = @Schema(description = "할 일"))
        List<TodoResponse> todos
) {
}
