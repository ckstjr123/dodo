package com.dodo.todo.todo.controller;

import com.dodo.todo.todo.dto.TodoRequest;
import com.dodo.todo.todo.dto.TodoCreateResponse;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoResponse;
import com.dodo.todo.todo.dto.TodoUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Todo", description = "Todo API")
public interface TodoApiDocs {

    @Operation(summary = "Todo 생성")
    @SecurityRequirement(name = "bearerAuth")
    TodoCreateResponse createTodo(Long memberId, TodoRequest request);

    @Operation(summary = "Todo 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    TodoListResponse getTodos(Long memberId);

    @Operation(summary = "Todo 단건 조회")
    @SecurityRequirement(name = "bearerAuth")
    TodoResponse getTodo(Long memberId, Long todoId);

    @Operation(summary = "Todo 수정")
    @SecurityRequirement(name = "bearerAuth")
    void updateTodo(Long memberId, Long todoId, TodoUpdateRequest request);

    @Operation(summary = "Todo 완료")
    @SecurityRequirement(name = "bearerAuth")
    void completeTodo(Long memberId, Long todoId);

    @Operation(summary = "Todo 완료 취소")
    @SecurityRequirement(name = "bearerAuth")
    void undoTodo(Long memberId, Long todoId);

    @Operation(summary = "Todo 삭제")
    @SecurityRequirement(name = "bearerAuth")
    void deleteTodo(Long memberId, Long todoId);
}
