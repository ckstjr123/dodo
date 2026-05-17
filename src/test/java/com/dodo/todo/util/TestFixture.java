package com.dodo.todo.util;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.reminder.domain.AbsoluteReminder;
import com.dodo.todo.reminder.domain.RelativeReminder;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public static Category createCategory(Long id, Member member, String name) {
        Category category = createCategory(member, name);
        setId(category, id);
        return category;
    }

    public static Todo createTodo(Member member, Category category, String title, TodoStatus status) {
        return createTodo(member, category, null, title, status, null);
    }

    public static Todo createTodo(Long id, Member member, Category category, String title, TodoStatus status) {
        Todo todo = createTodo(member, category, null, title, status, null);
        setId(todo, id);
        return todo;
    }

    public static Todo createScheduledTodo(
            Long id,
            Member member,
            String categoryName,
            String title,
            LocalDate scheduledDate,
            LocalTime scheduledTime
    ) {
        return createScheduledTodo(id, member, createCategory(member, categoryName), title, scheduledDate, scheduledTime);
    }

    public static Todo createScheduledTodo(
            Member member,
            String categoryName,
            String title,
            LocalDate scheduledDate,
            LocalTime scheduledTime
    ) {
        return createScheduledTodo(null, member, createCategory(member, categoryName), title, scheduledDate, scheduledTime);
    }

    private static Todo createScheduledTodo(
            Long id,
            Member member,
            Category category,
            String title,
            LocalDate scheduledDate,
            LocalTime scheduledTime
    ) {
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title(title)
                .status(TodoStatus.TODO)
                .scheduledDate(scheduledDate)
                .scheduledTime(scheduledTime)
                .build();
        if (id != null) {
            setId(todo, id);
        }
        return todo;
    }

    public static Todo createRecurringTodo(
            Long id,
            Member member,
            Category category,
            String title,
            TodoStatus status,
            LocalDate scheduledDate,
            TodoRecurrence recurrence
    ) {
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .mainTodo(null)
                .title(title)
                .status(status)
                .scheduledDate(scheduledDate)
                .recurrence(recurrence)
                .dueAt(null)
                .build();
        if (id != null) {
            setId(todo, id);
        }
        return todo;
    }

    public static Todo createRecurringTodo(
            Member member,
            Category category,
            String title,
            TodoStatus status,
            LocalDate scheduledDate,
            TodoRecurrence recurrence
    ) {
        return createRecurringTodo(null, member, category, title, status, scheduledDate, recurrence);
    }

    public static Todo createSubTodo(
            Member member,
            Category category,
            Todo mainTodo,
            String title,
            TodoStatus status
    ) {
        return createTodo(member, category, mainTodo, title, status, null);
    }

    public static RelativeReminder createRelativeReminder(Long id, Todo todo, Member member, int minuteOffset) {
        RelativeReminder reminder = RelativeReminder.create(todo, member, minuteOffset);
        setId(reminder, id);
        return reminder;
    }

    public static AbsoluteReminder createAbsoluteReminder(Long id, Todo todo, Member member, LocalDateTime due) {
        AbsoluteReminder reminder = AbsoluteReminder.create(todo, member, due);
        setId(reminder, id);
        return reminder;
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
