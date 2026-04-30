package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.recurrence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static com.dodo.todo.util.TestFixture.createRecurringTodo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoTest {

    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 4, 10, 12, 0);

    @Test
    @DisplayName("다른 회원의 Todo면 소유자가 아니다")
    void isNotOwnedByOtherMemberEntity() {
        Todo todo = todo(Member.of("member@example.com"));

        assertThat(todo.isOwnedBy(Member.of("other@example.com"))).isFalse();
    }

    @Test
    @DisplayName("메인 Todo가 있으면 subTodo다")
    void hasMainTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo mainTodo = Todo.builder()
                .member(member)
                .category(category)
                .title("main")
                .status(TodoStatus.TODO)
                .build();

        Todo subTodo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.TODO)
                .build();

        assertThat(subTodo.hasMainTodo()).isTrue();
        assertThat(subTodo.getMainTodo()).isSameAs(mainTodo);
    }

    @Test
    @DisplayName("다른 회원 Todo를 메인 Todo로 지정할 수 없다")
    void rejectNotOwnedMainTodo() {
        Member member = Member.of("member@example.com");
        Member otherMember = Member.of("other@example.com");
        Category category = Category.create(member, "work");
        Category otherCategory = Category.create(otherMember, "work");
        Todo mainTodo = Todo.builder()
                .member(otherMember)
                .category(otherCategory)
                .title("main")
                .status(TodoStatus.TODO)
                .build();

        assertThatThrownBy(() -> Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.TODO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(TodoError.MAIN_TODO_NOT_OWNED.message());
    }

    @Test
    @DisplayName("일반 Todo를 완료하면 DONE 상태가 된다")
    void completeNormalTodo() {
        Todo todo = todo(Member.of("member@example.com"));

        todo.complete(COMPLETED_AT);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.DONE);
    }

    @Test
    @DisplayName("이미 완료된 Todo를 다시 완료하면 예외가 발생한다")
    void rejectCompleteDoneTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("done")
                .status(TodoStatus.DONE)
                .build();

        assertThatThrownBy(() -> todo.complete(COMPLETED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TodoError.TODO_ALREADY_COMPLETED.message());
    }

    @Test
    @DisplayName("반복 Todo는 scheduledDate 없이 생성할 수 없다")
    void rejectRecurringTodoWithoutScheduledDate() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");

        assertThatThrownBy(() -> Todo.builder()
                .member(member)
                .category(category)
                .title("recurring")
                .status(TodoStatus.TODO)
                .recurrenceRule(new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null, RecurrenceCriteria.SCHEDULED_DATE))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(TodoError.RECURRING_TODO_SCHEDULED_DATE_REQUIRED.message());
    }

    @Test
    @DisplayName("반복 Todo를 완료하면 다음 스케줄로 이동한다")
    void completeRecurringTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 7);
        Todo todo = createRecurringTodo(
                1L,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.WEEKLY, 1, WeekDays.of(0, List.of(Day.FR)), null, null, RecurrenceCriteria.SCHEDULED_DATE)
        );

        todo.complete(COMPLETED_AT);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(scheduledDate.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)));
        assertThat(todo.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    @DisplayName("완료일 기준 반복 Todo는 완료일을 기준으로 다음 스케줄로 이동한다")
    void completeRecurringTodoByCompletedDateCriteria() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 28);
        LocalDateTime completedAt = LocalDateTime.of(2026, 4, 30, 12, 0);
        Todo todo = createRecurringTodo(
                1L,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null, RecurrenceCriteria.COMPLETED_DATE)
        );

        todo.complete(completedAt);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(completedAt.toLocalDate().plusDays(1));
        assertThat(todo.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("완료일 기준 반복 Todo는 예정일 전에는 완료할 수 없다")
    void rejectCompleteCompletedDateCriteriaTodoBeforeScheduledDate() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = createRecurringTodo(
                1L,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                LocalDate.of(2026, 4, 30),
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null, RecurrenceCriteria.COMPLETED_DATE)
        );

        assertThatThrownBy(() -> todo.complete(LocalDateTime.of(2026, 4, 29, 12, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(RecurrenceRuleError.COMPLETION_BEFORE_SCHEDULED_DATE.message());
    }

    @Test
    @DisplayName("예정일 기준 반복 Todo는 예정일 전에도 완료할 수 있다")
    void completeScheduledDateCriteriaTodoBeforeScheduledDate() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 30);
        Todo todo = createRecurringTodo(
                1L,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null, RecurrenceCriteria.SCHEDULED_DATE)
        );

        todo.complete(LocalDateTime.of(2026, 4, 29, 12, 0));

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(scheduledDate.plusDays(1));
    }

    @Test
    @DisplayName("반복 종료일에 도달한 Todo를 완료하면 다음 일정이 없어 DONE 상태가 된다")
    void completeRecurringTodoOnUntilDate() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 10), until = scheduledDate;
        Todo todo = createRecurringTodo(
                1L,
                member,
                category,
                "recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, until, RecurrenceCriteria.SCHEDULED_DATE)
        );

        todo.complete(COMPLETED_AT);

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(todo.getScheduledDate()).isEqualTo(scheduledDate);
    }

    @Test
    @DisplayName("메인 Todo를 완료하면 하위 작업도 함께 완료된다")
    void completeMainTodoCompletesSubTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo mainTodo = Todo.builder()
                .member(member)
                .category(category)
                .title("main")
                .status(TodoStatus.TODO)
                .build();

        Todo subTodo = createRecurringTodo(
                1L,
                member,
                category,
                "sub recurring",
                TodoStatus.TODO,
                LocalDate.of(2026, 4, 7),
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null, RecurrenceCriteria.SCHEDULED_DATE)
        );
        setSubTodos(mainTodo, subTodo);

        mainTodo.complete(COMPLETED_AT);

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(mainTodo.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(subTodo.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    @DisplayName("반복 메인 Todo를 완료해 다음 일정으로 이동하면 하위 작업은 TODO로 초기화된다")
    void completeRecurringMainTodoResetsSubTodosWhenNextDateExists() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 7);
        Todo mainTodo = createRecurringTodo(
                1L,
                member,
                category,
                "main recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null, RecurrenceCriteria.SCHEDULED_DATE)
        );
        Todo subTodo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.DONE)
                .completedAt(COMPLETED_AT)
                .build();
        setSubTodos(mainTodo, subTodo);

        mainTodo.complete(COMPLETED_AT);

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(mainTodo.getScheduledDate()).isEqualTo(scheduledDate.plusDays(1));
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(subTodo.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("반복 메인 Todo가 영구 완료되면 하위 작업도 함께 완료된다")
    void completeRecurringMainTodoCompletesSubTodosWhenNextDateDoesNotExist() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        LocalDate scheduledDate = LocalDate.of(2026, 4, 10);
        Todo mainTodo = createRecurringTodo(
                1L,
                member,
                category,
                "main recurring",
                TodoStatus.TODO,
                scheduledDate,
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, scheduledDate, RecurrenceCriteria.SCHEDULED_DATE)
        );
        Todo subTodo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.TODO)
                .build();
        setSubTodos(mainTodo, subTodo);

        mainTodo.complete(COMPLETED_AT);

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(mainTodo.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(subTodo.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    @DisplayName("영구 완료된 반복 Todo를 TODO 상태로 복구할 수 있다")
    void undoRecurringDoneTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("recurring")
                .status(TodoStatus.DONE)
                .scheduledDate(LocalDate.of(2026, 4, 10))
                .completedAt(COMPLETED_AT)
                .recurrenceRule(new RecurrenceRule(
                        Frequency.DAILY,
                        1,
                        WeekDays.empty(),
                        null,
                        LocalDate.of(2026, 4, 10),
                        RecurrenceCriteria.SCHEDULED_DATE
                ))
                .build();

        todo.undo();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("완료된 메인 Todo를 완료 취소하면 하위 작업도 함께 TODO로 복구된다")
    void undoDoneMainTodoRestoresSubTodos() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo mainTodo = Todo.builder()
                .member(member)
                .category(category)
                .title("main")
                .status(TodoStatus.DONE)
                .completedAt(COMPLETED_AT)
                .build();
        Todo subTodo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.DONE)
                .completedAt(COMPLETED_AT)
                .build();
        setSubTodos(mainTodo, subTodo);

        mainTodo.undo();

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(mainTodo.getCompletedAt()).isNull();
        assertThat(subTodo.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("완료된 하위 Todo를 완료 취소하면 메인 Todo도 TODO로 복구하고 다른 하위 작업은 유지한다")
    void undoDoneSubTodoRestoresMainTodoOnly() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo mainTodo = Todo.builder()
                .member(member)
                .category(category)
                .title("main")
                .status(TodoStatus.DONE)
                .completedAt(COMPLETED_AT)
                .build();
        Todo subTodo1 = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.DONE)
                .completedAt(COMPLETED_AT)
                .build();
        Todo subTodo2 = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sibling")
                .status(TodoStatus.DONE)
                .completedAt(COMPLETED_AT)
                .build();
        setSubTodos(mainTodo, subTodo1, subTodo2);

        subTodo1.undo();

        assertAll(
                () -> assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.TODO),
                () -> assertThat(mainTodo.getCompletedAt()).isNull(),
                () -> assertThat(subTodo1.getStatus()).isEqualTo(TodoStatus.TODO),
                () -> assertThat(subTodo1.getCompletedAt()).isNull(),
                () -> assertThat(subTodo2.getStatus()).isEqualTo(TodoStatus.DONE),
                () -> assertThat(subTodo2.getCompletedAt()).isEqualTo(COMPLETED_AT)
        );
    }

    @Test
    @DisplayName("이미 TODO인 Todo를 완료 취소하면 예외가 발생한다")
    void rejectUndoNotCompletedTodo() {
        Todo todo = todo(Member.of("member@example.com"));

        assertThatThrownBy(todo::undo)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TodoError.TODO_NOT_COMPLETED.message());
    }

    private Todo todo(Member member) {
        Category category = Category.create(member, "work");

        return Todo.builder()
                .member(member)
                .category(category)
                .title("doc")
                .status(TodoStatus.TODO)
                .build();
    }

    private void setSubTodos(Todo mainTodo, Todo... subTodos) {
        ReflectionTestUtils.setField(mainTodo, "subTodos", List.of(subTodos));
    }
}
