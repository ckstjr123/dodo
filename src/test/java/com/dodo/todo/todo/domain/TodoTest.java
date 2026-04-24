package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoTest {

    @Test
    @DisplayName("같은 회원 엔티티면 소유자로 판단한다")
    void isOwnedBySameMemberEntity() {
        Member member = Member.of("member@example.com");
        Todo todo = todo(member);

        assertThat(todo.isOwnedBy(member)).isTrue();
    }

    @Test
    @DisplayName("다른 회원 엔티티면 소유자가 아니다")
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
    @DisplayName("반복 Todo를 완료하면 다음 스케줄로 이동한다")
    void completeRecurringTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("recurring")
                .status(TodoStatus.TODO)
                .scheduledDate(LocalDate.of(2026, 4, 7))
                .recurrenceRule(new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, null))
                .build();

        todo.complete();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.getScheduledDate()).isEqualTo(LocalDate.of(2026, 4, 8));
    }

    @Test
    @DisplayName("종료일이 지난 반복 Todo를 완료하면 DONE 상태가 된다")
    void completeRecurringTodoAfterUntil() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("recurring")
                .status(TodoStatus.TODO)
                .scheduledDate(LocalDate.of(2026, 4, 10))
                .recurrenceRule(new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, LocalDate.of(2026, 4, 10)))
                .build();

        todo.complete();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(todo.getScheduledDate()).isEqualTo(LocalDate.of(2026, 4, 10));
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

        Todo subTodo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title("sub recurring")
                .status(TodoStatus.TODO)
                .scheduledDate(LocalDate.of(2026, 4, 7))
                .recurrenceRule(new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, null))
                .build();
        ReflectionTestUtils.setField(mainTodo, "subTodos", List.of(subTodo));

        mainTodo.complete();

        assertThat(mainTodo.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(subTodo.getStatus()).isEqualTo(TodoStatus.DONE);
    }

    @Test
    @DisplayName("영구 완료된 반복 Todo도 TODO 상태로 복구할 수 있다")
    void undoRecurringDoneTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "work");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("recurring")
                .status(TodoStatus.DONE)
                .scheduledDate(LocalDate.of(2026, 4, 10))
                .recurrenceRule(new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, LocalDate.of(2026, 4, 10)))
                .build();

        todo.undo();

        assertThat(todo.getStatus()).isEqualTo(TodoStatus.TODO);
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
}