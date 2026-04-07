package com.dodo.todo.todo.controller;

import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Todo", description = "Todo API")
public interface TodoApiDocs {

    @Operation(summary = "Todo 생성")
    @SecurityRequirement(name = "bearerAuth")
    TodoResponse createTodo(Long memberId, TodoCreateRequest request);

    @Operation(summary = "Todo 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    TodoListResponse getTodos(Long memberId);

    @Operation(summary = "Todo 단건 조회")
    @SecurityRequirement(name = "bearerAuth")
    TodoResponse getTodo(Long memberId, Long todoId);
}
