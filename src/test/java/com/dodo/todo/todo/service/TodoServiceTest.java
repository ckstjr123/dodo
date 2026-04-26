package com.dodo.todo.todo.service;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.category.repository.CategoryRepository;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.dto.RecurrenceRuleRequest;
import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.repository.TodoHistoryRepository;
import com.dodo.todo.todo.repository.TodoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.dodo.todo.util.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private TodoHistoryRepository todoHistoryRepository;

    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("다른 회원 카테고리로 Todo를 생성할 수 없다")
    void saveTodoRejectsForeignCategory() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Member member = Member.of("member@example.com");
        Category category = Category.create(Member.of("other@example.com"), "work");

        when(memberService.findById(memberId)).thenReturn(member);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> todoService.saveTodo(memberId, createRequest(categoryId, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category not found");
    }

    @Test
    @DisplayName("메인 Todo를 생성한다")
    void saveMainTodoSuccess() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Long savedTodoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Todo savedTodo = createTodo(savedTodoId, member, category, "title", TodoStatus.TODO);

        when(memberService.findById(memberId)).thenReturn(member);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

        Long todoId = todoService.saveTodo(memberId, createRequest(categoryId, null, null));

        assertThat(todoId).isEqualTo(savedTodoId);
    }

    @Test
    @DisplayName("깊이가 2를 넘는 하위 작업은 생성할 수 없다")
    void saveTodoRejectsDepthLimitExceeded() {
        Long memberId = 1L;
        Long categoryId = 10L;
        Long mainTodoId = 8L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Todo mainTodo = createTodo(7L, member, category, "main", TodoStatus.TODO);
        Todo subTodo = createSubTodo(member, category, mainTodo, "sub", mainTodoId);

        when(memberService.findById(memberId)).thenReturn(member);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(todoRepository.findByIdAndMemberId(mainTodoId, memberId)).thenReturn(Optional.of(subTodo));

        assertThatThrownBy(() -> todoService.saveTodo(memberId, createRequest(categoryId, mainTodoId, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Todo depth must not exceed 2");
    }

    @Test
    @DisplayName("일반 Todo를 완료하면 DONE 상태가 된다")
    void completeNormalTodo() {
        Long memberId = 1L;
        Long todoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Todo todo = createTodo(todoId, member, category, "title", TodoStatus.TODO);

        when(todoRepository.findWithSubTodos(todoId, memberId)).thenReturn(Optional.of(todo));

        todoService.completeTodo(memberId, todoId);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.DONE);
        verify(todoHistoryRepository).save(any(TodoHistory.class));
    }

    @Test
    @DisplayName("반복 Todo를 완료하면 다음 일정으로 이동한다")
    void completeRecurringTodo() {
        Long memberId = 1L;
        Long todoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Todo todo = createRecurringTodo(
                todoId,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                LocalDate.of(2026, 4, 7),
                new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, null)
        );

        when(todoRepository.findWithSubTodos(todoId, memberId)).thenReturn(Optional.of(todo));

        todoService.completeTodo(memberId, todoId);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(LocalDate.of(2026, 4, 8));
        verify(todoHistoryRepository).save(any(TodoHistory.class));
    }

    @Test
    @DisplayName("반복 종료일에 도달한 Todo를 완료하면 다음 일정이 없어 DONE 상태가 된다")
    void completeRecurringTodoOnUntilDate() {
        Long memberId = 1L;
        Long todoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 10);
        Todo todo = createRecurringTodo(
                todoId,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, scheduledDate)
        );

        when(todoRepository.findWithSubTodos(todoId, memberId)).thenReturn(Optional.of(todo));

        todoService.completeTodo(memberId, todoId);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.DONE);
    }

    @Test
    @DisplayName("완전 완료된 반복 Todo를 완료 취소하면 TODO 상태로 복구된다")
    void undoRecurringDoneTodo() {
        Long memberId = 1L;
        Long todoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Todo todo = createRecurringTodo(
                todoId,
                member,
                category,
                "recurring",
                TodoStatus.DONE,
                LocalDate.of(2026, 4, 10),
                new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, LocalDate.of(2026, 5, 10))
        );

        when(todoRepository.findWithSubTodos(todoId, memberId)).thenReturn(Optional.of(todo));

        todoService.undoTodo(memberId, todoId);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
    }

    private TodoCreateRequest createRequest(Long categoryId, Long mainTodoId, RecurrenceRule recurrenceRule) {
        return new TodoCreateRequest(
                categoryId,
                mainTodoId,
                "title",
                "memo",
                1,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                LocalDate.of(2026, 4, 7),
                LocalTime.of(14, 0),
                createRecurrenceRuleRequest(recurrenceRule)
        );
    }

    private RecurrenceRuleRequest createRecurrenceRuleRequest(RecurrenceRule recurrenceRule) {
        if (recurrenceRule == null) {
            return null;
        }

        return new RecurrenceRuleRequest(
                recurrenceRule.frequency(),
                recurrenceRule.interval(),
                recurrenceRule.byDay(),
                recurrenceRule.byMonthDay(),
                recurrenceRule.until()
        );
    }
}
