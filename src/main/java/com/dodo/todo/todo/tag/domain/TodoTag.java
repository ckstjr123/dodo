package com.dodo.todo.todo.tag.domain;

import com.dodo.todo.tag.domain.Tag;
import com.dodo.todo.todo.domain.Todo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "todo_tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private TodoTag(Todo todo, Tag tag) {
        validateTodo(todo);
        validateTag(tag);
        this.todo = todo;
        this.tag = tag;
    }

    public static TodoTag create(Todo todo, Tag tag) {
        return new TodoTag(todo, tag);
    }

    public Long getTodoId() {
        return todo.getId();
    }

    public Long getTagId() {
        return tag.getId();
    }

    private void validateTodo(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo is required");
        }
    }

    private void validateTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag is required");
        }
    }
}
