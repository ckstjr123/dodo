package com.dodo.todo.category.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "카테고리 목록 응답")
public record CategoryListResponse(
        @ArraySchema(schema = @Schema(description = "카테고리"))
        List<CategoryResponse> categories
) {
}
