package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record TodoResponse(
        Long id,
        Long mainTodoId,
        Long categoryId,
        String categoryName,
        String title,
        String memo,
        TodoStatus status,
        int sortOrder,
        LocalDateTime dueAt,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        RecurrenceRule recurrenceRule,
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
                todo.getRecurrenceRule(),
                todo.getSubTodos().stream()
                        .map(TodoResponse::from)
                        .toList()
        );
    }
}
