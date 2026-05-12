package com.dodo.todo.todo.service;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.category.service.CategoryService;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.reminder.service.ReminderService;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoError;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import com.dodo.todo.todo.dto.TodoDetailResponse;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoRequest;
import com.dodo.todo.todo.dto.TodoResponse;
import com.dodo.todo.todo.dto.TodoUpdateRequest;
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
    private final CategoryService categoryService;
    private final TodoRepository todoRepository;
    private final TodoHistoryRepository todoHistoryRepository;
    private final ReminderService reminderService;

    @Transactional
    public Long saveTodo(Long memberId, TodoRequest request) {
        Member member = memberService.findById(memberId);
        Category category = categoryService.findByIdAndMemberId(request.categoryId(), memberId);
        Todo mainTodo = findMainTodo(request.parentTodoId(), memberId);
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

        Todo savedTodo = todoRepository.save(todo);
        reminderService.createReminders(savedTodo, member, request.reminders());

        return savedTodo.getId();
    }

    @Transactional(readOnly = true)
    public TodoListResponse getTodos(Long memberId) {
        List<TodoResponse> todos = todoRepository.findWithSubTodos(memberId).stream()
                .map(TodoResponse::from)
                .toList();

        return new TodoListResponse(todos);
    }

    @Transactional(readOnly = true)
    public TodoDetailResponse getTodo(Long todoId, Long memberId) {
        Todo todo = findTodoWithSubTodos(todoId, memberId);
        return TodoDetailResponse.from(todo);
    }

    /**
     * Todo 기본 정보 수정
     * 상태와 완료 이력은 별도 완료/취소 기능에서만 변경한다.
     */
    @Transactional
    public void updateTodo(Long todoId, Long memberId, TodoUpdateRequest request) {
        memberService.findById(memberId);
        Todo todo = findTodo(todoId, memberId);
        Category category = categoryService.findByIdAndMemberId(request.categoryId(), memberId);
        TodoRecurrence recurrence = request.getRecurrence();

        todo.updateDetails(
                category,
                request.title(),
                request.memo(),
                request.sortOrder(),
                request.dueAt(),
                request.scheduledDate(),
                request.scheduledTime(),
                recurrence
        );

        if (request.scheduledDate() == null || request.scheduledTime() == null) {
            reminderService.deleteRemindersByTodoId(todo.getId());
        }
    }

    @Transactional
    public void completeTodo(Long todoId, Long memberId) {
        Todo todo = findTodoWithSubTodos(todoId, memberId);
        LocalDateTime completedAt = LocalDateTime.now();
        todo.complete(completedAt);
        todoHistoryRepository.save(TodoHistory.create(todo, completedAt));
    }

    @Transactional
    public void undoTodo(Long todoId, Long memberId) {
        Todo todo = findTodoWithSubTodos(todoId, memberId);
        todo.undo();
    }

    /**
     * Todo 삭제
     * 메인 Todo 삭제 시 하위 Todo를 먼저 삭제해 부모 참조 제약을 지킨다.
     */
    @Transactional
    public void deleteTodo(Long todoId, Long memberId) {
        Todo todo = findTodo(todoId, memberId);

        if (!todo.hasMainTodo()) {
            reminderService.deleteRemindersByParentTodoId(todo.getId());
            todoRepository.deleteSubTodosByParentId(todo.getId());
        }

        reminderService.deleteRemindersByTodoId(todo.getId());
        todoRepository.deleteById(todo.getId());
    }

    private Todo findMainTodo(Long todoId, Long memberId) {
        if (todoId == null) {
            return null;
        }

        Todo todo = todoRepository.findByIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));

        if (todo.hasMainTodo()) {
            throw new BusinessException(TodoError.TODO_DEPTH_LIMIT_EXCEEDED);
        }

        return todo;
    }

    private Todo findTodo(Long todoId, Long memberId) {
        return todoRepository.findByIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));
    }

    private Todo findTodoWithSubTodos(Long todoId, Long memberId) {
        return todoRepository.findWithSubTodos(todoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));
    }
}
