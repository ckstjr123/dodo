package com.dodo.todo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "할 일 생성 응답")
public record TodoCreateResponse(
        @Schema(description = "생성된 할 일 ID", example = "1")
        Long todoId
) {
}
