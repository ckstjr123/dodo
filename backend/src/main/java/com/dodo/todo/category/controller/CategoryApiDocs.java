package com.dodo.todo.category.controller;

import com.dodo.todo.category.dto.CategoryCreateResponse;
import com.dodo.todo.category.dto.CategoryListResponse;
import com.dodo.todo.category.dto.CategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Category", description = "Category API")
public interface CategoryApiDocs {

    @Operation(summary = "카테고리 생성")
    @SecurityRequirement(name = "bearerAuth")
    CategoryCreateResponse createCategory(Long memberId, CategoryRequest request);

    @Operation(summary = "카테고리 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    CategoryListResponse getCategories(Long memberId);

    @Operation(summary = "카테고리 수정")
    @SecurityRequirement(name = "bearerAuth")
    void updateCategory(Long memberId, Long categoryId, CategoryRequest request);

    @Operation(summary = "카테고리 삭제")
    @SecurityRequirement(name = "bearerAuth")
    void deleteCategory(Long memberId, Long categoryId);
}
