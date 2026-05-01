package com.dodo.todo.todo.service;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.category.repository.CategoryRepository;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoError;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import com.dodo.todo.todo.dto.TodoRequest;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoResponse;
import com.dodo.todo.todo.repository.TodoHistoryRepository;
import com.dodo.todo.todo.repository.TodoRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    public Long saveTodo(Long memberId, TodoRequest request) {
        Member member = memberService.findById(memberId);
        Category category = findCategory(member, request.categoryId());
        Todo mainTodo = findMainTodo(memberId, request.parentTodoId());
        TodoRecurrence recurrence = request.getRecurrence();

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
                .recurrence(recurrence)
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
        LocalDateTime completedAt = LocalDateTime.now();
        todo.complete(completedAt);
        todoHistoryRepository.save(TodoHistory.create(todo, completedAt));
    }

    @Transactional
    public void undoTodo(Long memberId, Long todoId) {
        Todo todo = findTodoWithSubTodos(memberId, todoId);
        todo.undo();
    }

    private Category findCategory(Member member, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(TodoError.CATEGORY_NOT_FOUND));

        if (!category.isOwnedBy(member)) {
            throw new BusinessException(TodoError.CATEGORY_NOT_FOUND);
        }

        return category;
    }

    private Todo findMainTodo(Long memberId, Long mainTodoId) {
        if (mainTodoId == null) {
            return null;
        }

        Todo todo = todoRepository.findByIdAndMemberId(mainTodoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));

        if (todo.hasMainTodo()) {
            throw new BusinessException(TodoError.TODO_DEPTH_LIMIT_EXCEEDED);
        }

        return todo;
    }

    private Todo findTodoWithSubTodos(Long memberId, Long todoId) {
        return todoRepository.findWithSubTodos(todoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));
    }
}
