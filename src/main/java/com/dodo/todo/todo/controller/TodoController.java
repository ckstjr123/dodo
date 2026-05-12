package com.dodo.todo.todo.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.todo.dto.TodoCreateResponse;
import com.dodo.todo.todo.dto.TodoDetailResponse;
import com.dodo.todo.todo.dto.TodoRequest;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoUpdateRequest;
import com.dodo.todo.todo.service.TodoService;
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
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
public class TodoController implements TodoApiDocs {

    private final TodoService todoService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TodoCreateResponse createTodo(@LoginMember Long memberId, @Valid @RequestBody TodoRequest request) {
        return new TodoCreateResponse(todoService.saveTodo(memberId, request));
    }

    @Override
    @GetMapping
    public TodoListResponse getTodos(@LoginMember Long memberId) {
        return todoService.getTodos(memberId);
    }

    @Override
    @GetMapping("/{todoId}")
    public TodoDetailResponse getTodo(@LoginMember Long memberId, @PathVariable Long todoId) {
        return todoService.getTodo(todoId, memberId);
    }

    @Override
    @PatchMapping("/{todoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTodo(@LoginMember Long memberId,
                           @PathVariable Long todoId,
                           @Valid @RequestBody TodoUpdateRequest request) {
        todoService.updateTodo(todoId, memberId, request);
    }

    @Override
    @PatchMapping("/{todoId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTodo(@LoginMember Long memberId, @PathVariable Long todoId) {
        todoService.completeTodo(todoId, memberId);
    }

    @Override
    @PatchMapping("/{todoId}/undo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void undoTodo(@LoginMember Long memberId, @PathVariable Long todoId) {
        todoService.undoTodo(todoId, memberId);
    }

    @Override
    @DeleteMapping("/{todoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTodo(@LoginMember Long memberId, @PathVariable Long todoId) {
        todoService.deleteTodo(todoId, memberId);
    }
}
