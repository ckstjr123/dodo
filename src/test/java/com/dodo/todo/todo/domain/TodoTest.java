package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.domain.recurrence.WeekDays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import net.fortuna.ical4j.model.WeekDay;

import static com.dodo.todo.util.TestFixture.createRecurringTodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoTest {

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
                .hasMessage("Main todo must belong to the same member");
    }

    @Test
    @DisplayName("일반 Todo를 완료하면 DONE 상태가 된다")
    void completeNormalTodo() {
        Todo todo = todo(Member.of("member@example.com"));

        todo.complete();

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

        assertThatThrownBy(todo::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Todo already completed");
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
                .recurrenceRule(new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled date is required for recurring todo");
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
                new RecurrenceRule(Frequency.WEEKLY, 1, WeekDays.from(List.of(WeekDay.FR)), null, null)
        );

        todo.complete();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(scheduledDate.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)));
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
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, until)
        );

        todo.complete();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(todo.getScheduledDate()).isEqualTo(scheduledDate);
    }

    @Test
    @DisplayName("메인 Todo를 완료하면 하위 작업도 함께 완료된다")
    void completeMainTodoCompletesRecurringSubTodo() {
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
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, null)
        );
        ReflectionTestUtils.setField(subTodo, "mainTodo", mainTodo);
        setSubTodos(mainTodo, subTodo);

        mainTodo.complete();

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.DONE);
    }

    @Test
    @DisplayName("영구 완료된 반복 Todo도 TODO 상태로 복구할 수 있다")
    void undoRecurringDoneTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = createRecurringTodo(
                1L,
                member,
                category,
                "recurring",
                TodoStatus.DONE,
                LocalDate.of(2026, 4, 10),
                new RecurrenceRule(Frequency.DAILY, 1, WeekDays.empty(), null, LocalDate.of(2026, 5, 10))
        );

        todo.undo();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
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
                .build();
        Todo subTodo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub")
                .status(TodoStatus.DONE)
                .build();
        setSubTodos(mainTodo, subTodo);

        mainTodo.undo();

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.TODO);
    }

    @Test
    @DisplayName("이미 TODO인 Todo를 완료 취소하면 예외가 발생한다")
    void rejectUndoNotCompletedTodo() {
        Todo todo = todo(Member.of("member@example.com"));

        assertThatThrownBy(todo::undo)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Todo is not completed");
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
