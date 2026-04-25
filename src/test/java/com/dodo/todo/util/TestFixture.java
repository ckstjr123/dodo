package com.dodo.todo.util;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.Frequency;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
        return createTodo(member, category, null, title, null, null);
    }

    public static Todo createTodo(Member member, Category category, String title, Long id) {
        Todo todo = createTodo(member, category, title);
        setId(todo, id);
        return todo;
    }

    public static Todo createRecurringTodo(Member member, Category category, String title, Long id) {
        Todo todo = createTodo(
                member,
                category,
                null,
                title,
                new RecurrenceRule(Frequency.DAILY, 1, List.of(), null, null),
                null
        );
        setId(todo, id);
        return todo;
    }

    public static Todo createSubTodo(Member member, Category category, Todo mainTodo, String title, Long id) {
        Todo todo = createTodo(member, category, mainTodo, title, null, null);
        setId(todo, id);
        return todo;
    }

    private static Todo createTodo(
            Member member,
            Category category,
            Todo mainTodo,
            String title,
            RecurrenceRule recurrenceRule,
            LocalDateTime dueAt
    ) {
        return Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(mainTodo)
                .title(title)
                .status(TodoStatus.TODO)
                .scheduledDate(recurrenceRule == null ? null : LocalDate.of(2026, 4, 7))
                .recurrenceRule(recurrenceRule)
                .dueAt(dueAt)
                .build();
    }

    public static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}