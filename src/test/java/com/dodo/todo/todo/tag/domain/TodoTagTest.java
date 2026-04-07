package com.dodo.todo.todo.tag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.tag.domain.Tag;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TodoTagTest {

    @Test
    @DisplayName("Todo에 태그를 추가하면 태그 매핑을 함께 보관한다")
    void addTagAndRegisterOnTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("문서 정리")
                .status(TodoStatus.OPEN)
                .priority("MEDIUM")
                .build();
        Tag tag = Tag.create(member, "중요");

        todo.addTag(tag);

        TodoTag todoTag = todo.getTodoTags().get(0);
        assertThat(todoTag.getTodo()).isSameAs(todo);
        assertThat(todoTag.getTag()).isSameAs(tag);
    }

    @Test
    @DisplayName("TodoTag는 태그 없이 생성할 수 없다")
    void rejectTodoTagWithoutTag() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");
        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title("문서 정리")
                .status(TodoStatus.OPEN)
                .priority("MEDIUM")
                .build();

        assertThatThrownBy(() -> todo.addTag(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag is required");
    }
}
