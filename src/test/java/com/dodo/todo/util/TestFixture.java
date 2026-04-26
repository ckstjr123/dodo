package com.dodo.todo.util;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.test.util.ReflectionTestUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestFixture {

    public static Member createMember(Long id) {
        return createMember(id, "member@example.com");
    }

    public static Member createMember(Long id, String email) {
        Member member = Member.of(email);
        setId(member, id);
        return member;
    }

    public static Category createCategory(Member member, String name) {
        return Category.create(member, name);
    }

    public static Todo createTodo(Member member, Category category, String title) {
        return createTodo(member, category, null, title, TodoStatus.TODO, null);
    }

    public static Todo createTodo(Long id, Member member, Category category, String title, TodoStatus status) {
        Todo todo = createTodo(member, category, null, title, status, null);
        setId(todo, id);
        return todo;
    }

    public static Todo createRecurringTodo(
            Long id,
            Member member,
            Category category,
            String title,
            TodoStatus status,
            LocalDate scheduledDate,
            RecurrenceRule recurrenceRule
    ) {
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(null)
                .title(title)
                .status(status)
                .scheduledDate(scheduledDate)
                .recurrenceRule(recurrenceRule)
                .dueAt(null)
                .build();
        setId(todo, id);
        return todo;
    }

    public static Todo createSubTodo(Member member, Category category, Todo mainTodo, String title, Long id) {
        Todo todo = createTodo(member, category, mainTodo, title, TodoStatus.TODO, null);
        setId(todo, id);
        return todo;
    }

    private static Todo createTodo(
            Member member,
            Category category,
            Todo mainTodo,
            String title,
            TodoStatus status,
            LocalDateTime dueAt
    ) {
        return Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title(title)
                .status(status)
                .dueAt(dueAt)
                .build();
    }

    private static void setId(Object target, long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
