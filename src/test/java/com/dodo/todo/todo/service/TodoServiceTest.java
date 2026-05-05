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
import com.dodo.todo.recurrencerule.Frequency;
import com.dodo.todo.recurrencerule.RecurrenceRule;
import com.dodo.todo.recurrencerule.WeekDays;
import com.dodo.todo.todo.domain.recurrence.RecurrenceCriteria;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import com.dodo.todo.todo.dto.ByDayRequest;
import com.dodo.todo.todo.dto.RecurrenceRuleRequest;
import com.dodo.todo.todo.dto.TodoRecurrenceRequest;
import com.dodo.todo.todo.dto.TodoRequest;
import com.dodo.todo.todo.dto.TodoUpdateRequest;
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

        assertThatThrownBy(() -> todoService.saveTodo(
                memberId,
                createTodoRequest(categoryId, null, LocalDate.of(2026, 4, 7), null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TodoError.CATEGORY_NOT_FOUND.message());
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

        Long todoId = todoService.saveTodo(
                memberId,
                createTodoRequest(categoryId, null, LocalDate.of(2026, 4, 7), null)
        );

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
        Todo subTodo = createSubTodo(member, category, mainTodo, "sub", TodoStatus.TODO, mainTodoId);

        when(memberService.findById(memberId)).thenReturn(member);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(todoRepository.findByIdAndMemberId(mainTodoId, memberId)).thenReturn(Optional.of(subTodo));

        assertThatThrownBy(() -> todoService.saveTodo(
                memberId,
                createTodoRequest(categoryId, mainTodoId, LocalDate.of(2026, 4, 7), null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TodoError.TODO_DEPTH_LIMIT_EXCEEDED.message());
    }

    @Test
    @DisplayName("일반 Todo를 완료하면 DONE 상태가 되고 완료 이력을 남긴다")
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
    @DisplayName("반복 Todo를 완료하면 다음 일정으로 이동하고 완료 이력을 남긴다")
    void completeRecurringTodo() {
        Long memberId = 1L;
        Long todoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 7);
        Todo todo = createRecurringTodo(
                todoId,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                recurrence(
                        new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null),
                        RecurrenceCriteria.SCHEDULED_DATE
                )
        );

        when(todoRepository.findWithSubTodos(todoId, memberId)).thenReturn(Optional.of(todo));

        todoService.completeTodo(memberId, todoId);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(scheduledDate.plusDays(1));
        verify(todoHistoryRepository).save(any(TodoHistory.class));
    }

    @Test
    @DisplayName("반복 종료일에 도달한 Todo를 완료하면 다음 일정이 없어 DONE 상태가 된다")
    void completeRecurringTodoOnUntilDate() {
        Long memberId = 1L;
        Long todoId = 7L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 10), until = scheduledDate;
        Todo todo = createRecurringTodo(
                todoId,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                recurrence(
                        new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, until),
                        RecurrenceCriteria.SCHEDULED_DATE
                )
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
                recurrence(
                        new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, LocalDate.of(2026, 5, 10)),
                        RecurrenceCriteria.SCHEDULED_DATE
                )
        );

        when(todoRepository.findWithSubTodos(todoId, memberId)).thenReturn(Optional.of(todo));

        todoService.undoTodo(memberId, todoId);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
    }

    @Test
    @DisplayName("완료된 하위 Todo를 완료 취소하면 메인 Todo도 TODO로 복구한다")
    void undoSubTodoRestoresMainTodo() {
        Long memberId = 1L;
        Long subTodoId = 8L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Todo mainTodo = createTodo(7L, member, category, "main", TodoStatus.DONE);
        Todo subTodo = createSubTodo(member, category, mainTodo, "sub", TodoStatus.DONE, subTodoId);

        when(todoRepository.findWithSubTodos(subTodoId, memberId)).thenReturn(Optional.of(subTodo));

        todoService.undoTodo(memberId, subTodoId);

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.TODO);
    }

    @Test
    @DisplayName("Todo 기본 정보를 수정한다")
    void updateTodoSuccess() {
        Long memberId = 1L;
        Long todoId = 7L;
        Long categoryId = 10L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Category updatedCategory = createCategory(member, "private");
        Todo todo = createTodo(todoId, member, category, "before", TodoStatus.TODO);
        TodoUpdateRequest request = createTodoUpdateRequest(categoryId, LocalDate.of(2026, 4, 8), recurrence(
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null),
                RecurrenceCriteria.SCHEDULED_DATE
        ));

        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(updatedCategory));

        todoService.updateTodo(memberId, todoId, request);

        assertThat(todo.getCategory()).isSameAs(updatedCategory);
        assertThat(todo.getTitle()).isEqualTo("updated title");
        assertThat(todo.getMemo()).isEqualTo("updated memo");
        assertThat(todo.getScheduledDate()).isEqualTo(LocalDate.of(2026, 4, 8));
        assertThat(todo.getRecurrence()).isEqualTo(request.getRecurrence());
    }

    @Test
    @DisplayName("다른 회원의 Todo는 수정할 수 없다")
    void updateTodoRejectsForeignTodo() {
        Long memberId = 1L;
        Long todoId = 7L;

        when(memberService.findById(memberId)).thenReturn(createMember(memberId));
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.updateTodo(
                memberId,
                todoId,
                createTodoUpdateRequest(10L, LocalDate.of(2026, 4, 8), null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TodoError.TODO_NOT_FOUND.message());
    }

    @Test
    @DisplayName("다른 회원 카테고리로 Todo를 수정할 수 없다")
    void updateTodoRejectsForeignCategory() {
        Long memberId = 1L;
        Long todoId = 7L;
        Long categoryId = 10L;
        Member member = createMember(memberId);
        Category category = createCategory(member, "work");
        Category foreignCategory = createCategory(Member.of("other@example.com"), "private");
        Todo todo = createTodo(todoId, member, category, "title", TodoStatus.TODO);

        when(memberService.findById(memberId)).thenReturn(member);
        when(todoRepository.findByIdAndMemberId(todoId, memberId)).thenReturn(Optional.of(todo));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(foreignCategory));

        assertThatThrownBy(() -> todoService.updateTodo(
                memberId,
                todoId,
                createTodoUpdateRequest(categoryId, LocalDate.of(2026, 4, 8), null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TodoError.CATEGORY_NOT_FOUND.message());
    }

    private TodoRequest createTodoRequest(
            Long categoryId,
            Long mainTodoId,
            LocalDate scheduledDate,
            TodoRecurrence recurrence
    ) {
        return new TodoRequest(
                categoryId,
                mainTodoId,
                "title",
                "memo",
                1,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                scheduledDate,
                LocalTime.of(14, 0),
                createTodoRecurrenceRequest(recurrence)
        );
    }

    private TodoUpdateRequest createTodoUpdateRequest(
            Long categoryId,
            LocalDate scheduledDate,
            TodoRecurrence recurrence
    ) {
        return new TodoUpdateRequest(
                categoryId,
                "updated title",
                "updated memo",
                2,
                LocalDateTime.of(2026, 4, 8, 18, 0),
                scheduledDate,
                LocalTime.of(15, 0),
                createTodoRecurrenceRequest(recurrence)
        );
    }

    private TodoRecurrenceRequest createTodoRecurrenceRequest(TodoRecurrence recurrence) {
        if (recurrence == null) {
            return null;
        }

        RecurrenceRule rule = recurrence.rule();
        return new TodoRecurrenceRequest(
                new RecurrenceRuleRequest(
                        rule.frequency(),
                        rule.interval(),
                        rule.byDay().isEmpty()
                        ? null
                        : new ByDayRequest(rule.byDay().offset(), rule.byDay().days()),
                        rule.byMonthDay(),
                        rule.until()
                ),
                recurrence.criteria()
        );
    }

    private TodoRecurrence recurrence(RecurrenceRule rule, RecurrenceCriteria criteria) {
        return new TodoRecurrence(rule, criteria);
    }
}
