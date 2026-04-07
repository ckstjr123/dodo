package com.dodo.todo.todo.checklist.domain;

import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.todo.domain.Todo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(name = "checklist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private Checklist(Todo todo, String content) {
        validateTodo(todo);
        validateContent(content);
        this.todo = todo;
        this.content = content;
    }

    public static Checklist create(Todo todo, String content) {
        return new Checklist(todo, content);
    }

    public Long getTodoId() {
        return todo.getId();
    }

    public void complete(LocalDateTime completedAt) {
        if (completedAt == null) {
            throw new IllegalArgumentException("Completed at is required");
        }
        this.completed = true;
        this.completedAt = completedAt;
    }

    private void validateTodo(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo is required");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
    }
}
