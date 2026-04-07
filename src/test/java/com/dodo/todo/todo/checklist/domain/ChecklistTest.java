package com.dodo.todo.todo.checklist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChecklistTest {

    @Test
    @DisplayName("체크리스트 항목은 Todo 하위 항목으로 생성된다")
    void createChecklist() {
        Todo todo = todo();
        Checklist checklist = Checklist.create(todo, "문서 정리");

        assertThat(checklist.getTodo()).isSameAs(todo);
        assertThat(checklist.getContent()).isEqualTo("문서 정리");
        assertThat(checklist.isCompleted()).isFalse();
        assertThat(checklist.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("체크리스트 항목은 완료 처리할 수 있다")
    void completeChecklist() {
        Checklist checklist = Checklist.create(todo(), "문서 정리");
        LocalDateTime completedAt = LocalDateTime.of(2026, 4, 7, 10, 0);

        checklist.complete(completedAt);

        assertThat(checklist.isCompleted()).isTrue();
        assertThat(checklist.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("체크리스트 내용은 비어 있을 수 없다")
    void rejectBlankContent() {
        assertThatThrownBy(() -> Checklist.create(todo(), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content is required");
    }

    @Test
    @DisplayName("체크리스트 완료 시각은 필요하다")
    void rejectChecklistCompletionWithoutTimestamp() {
        Checklist checklist = Checklist.create(todo(), "문서 정리");

        assertThatThrownBy(() -> checklist.complete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Completed at is required");
    }

    private Todo todo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");

        return Todo.builder()
                .member(member)
                .category(category)
                .title("문서 정리")
                .status(TodoStatus.OPEN)
                .priority("MEDIUM")
                .build();
    }
}
