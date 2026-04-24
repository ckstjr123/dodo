package com.dodo.todo.todo.domain;

import com.dodo.todo.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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
@Table(name = "todo_history")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private TodoHistory(Todo todo, Member member, LocalDateTime completedAt) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo is required");
        }
        if (member == null) {
            throw new IllegalArgumentException("Member is required");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("Completed at is required");
        }

        this.todo = todo;
        this.member = member;
        this.title = todo.getTitle();
        this.completedAt = completedAt;
    }

    /** 현재 todo title을 사용해 완료 이력을 생성한다. */
    public static TodoHistory create(Todo todo, LocalDateTime completedAt) {
        return new TodoHistory(todo, todo.getMember(), completedAt);
    }

    public Long getTodoId() {
        return todo.getId();
    }

    public Long getMemberId() {
        return member.getId();
    }
}