package com.dodo.todo.category.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.category.dto.CategoryCreateResponse;
import com.dodo.todo.category.dto.CategoryListResponse;
import com.dodo.todo.category.dto.CategoryRequest;
import com.dodo.todo.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController implements CategoryApiDocs {

    private final CategoryService categoryService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryCreateResponse createCategory(@LoginMember Long memberId,
                                                 @Valid @RequestBody CategoryRequest request) {
        return new CategoryCreateResponse(categoryService.createCategory(memberId, request));
    }

    @Override
    @GetMapping
    public CategoryListResponse getCategories(@LoginMember Long memberId) {
        return categoryService.getCategories(memberId);
    }

    @Override
    @PatchMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCategory(@LoginMember Long memberId,
                               @PathVariable Long categoryId,
                               @Valid @RequestBody CategoryRequest request) {
        categoryService.updateCategory(memberId, categoryId, request);
    }

    @Override
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@LoginMember Long memberId, @PathVariable Long categoryId) {
        categoryService.deleteCategory(memberId, categoryId);
    }
}
