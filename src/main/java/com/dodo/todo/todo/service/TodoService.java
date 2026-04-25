package com.dodo.todo.todo.service;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.category.repository.CategoryRepository;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoResponse;
import com.dodo.todo.todo.repository.TodoHistoryRepository;
import com.dodo.todo.todo.repository.TodoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final MemberService memberService;
    private final CategoryRepository categoryRepository;
    private final TodoRepository todoRepository;
    private final TodoHistoryRepository todoHistoryRepository;

    @Transactional
    public Long saveTodo(Long memberId, TodoCreateRequest request) {
        Member member = memberService.findById(memberId);
        Category category = findCategory(member, request.categoryId());
        Todo mainTodo = findMainTodo(memberId, request.parentTodoId());
        RecurrenceRule recurrenceRule = request.getRecurrenceRule();
        validateRecurrenceSchedule(recurrenceRule, request.scheduledDate());

        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title(request.title())
                .memo(request.memo())
                .status(TodoStatus.TODO)
                .sortOrder(request.sortOrder())
                .dueAt(request.dueAt())
                .scheduledDate(request.scheduledDate())
                .scheduledTime(request.scheduledTime())
                .recurrenceRule(recurrenceRule)
                .build();

        return todoRepository.save(todo).getId();
    }

    @Transactional(readOnly = true)
    public TodoListResponse getTodos(Long memberId) {
        List<TodoResponse> todos = todoRepository.findWithSubTodos(memberId).stream()
                .map(TodoResponse::from)
                .toList();

        return new TodoListResponse(todos);
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(Long memberId, Long todoId) {
        Todo todo = findTodoWithSubTodos(memberId, todoId);
        return TodoResponse.from(todo);
    }

    @Transactional
    public void completeTodo(Long memberId, Long todoId) {
        Todo todo = findTodoWithSubTodos(memberId, todoId);
        todo.complete();
        todoHistoryRepository.save(TodoHistory.create(todo, LocalDateTime.now()));
    }

    @Transactional
    public void undoTodo(Long memberId, Long todoId) {
        Todo todo = findTodoWithSubTodos(memberId, todoId);
        todo.undo();
    }

    private Category findCategory(Member member, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Category not found"));

        if (!category.isOwnedBy(member)) {
            throw new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Category not found");
        }

        return category;
    }

    private Todo findMainTodo(Long memberId, Long mainTodoId) {
        if (mainTodoId == null) {
            return null;
        }

        Todo todo = todoRepository.findByIdAndMemberId(mainTodoId, memberId)
                .orElseThrow(() -> new ApiException("TODO_NOT_FOUND", HttpStatus.NOT_FOUND, "Todo not found"));

        if (todo.hasMainTodo()) {
            throw new ApiException("TODO_DEPTH_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "Todo depth must not exceed 2");
        }

        return todo;
    }

    private Todo findTodoWithSubTodos(Long memberId, Long todoId) {
        return todoRepository.findWithSubTodos(todoId, memberId)
                .orElseThrow(() -> new ApiException("TODO_NOT_FOUND", HttpStatus.NOT_FOUND, "Todo not found"));
    }

    private void validateRecurrenceSchedule(RecurrenceRule recurrenceRule, LocalDate scheduledDate) {
        if (recurrenceRule != null && scheduledDate == null) {
            throw new ApiException("SCHEDULED_DATE_REQUIRED", HttpStatus.BAD_REQUEST, "Scheduled date is required for recurring todo");
        }
    }
}
