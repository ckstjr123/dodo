package com.dodo.todo.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카테고리 요청")
public record CategoryRequest(
        @Schema(description = "카테고리명", example = "개인")
        @NotBlank
        @Size(max = 100)
        String name
) {
}
