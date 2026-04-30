package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record TodoResponse(
        Long todoId,
        Long parentTodoId,
        Long categoryId,
        String categoryName,
        String title,
        String memo,
        TodoStatus status,
        int sortOrder,
        LocalDateTime dueAt,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        RecurrenceRuleResponse recurrenceRule,
        List<TodoResponse> subTodos
) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getMainTodoId(),
                todo.getCategoryId(),
                todo.getCategory().getName(),
                todo.getTitle(),
                todo.getMemo(),
                todo.getStatus(),
                todo.getSortOrder(),
                todo.getDueAt(),
                todo.getScheduledDate(),
                todo.getScheduledTime(),
                RecurrenceRuleResponse.from(todo.getRecurrenceRule()),
                todo.getSubTodos().stream()
                        .map(TodoResponse::from)
                        .toList()
        );
    }
}
