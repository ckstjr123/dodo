package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRuleConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@Table(name = "todo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_todo_id")
    private Todo mainTodo;

    @BatchSize(size = 100)
    @OrderBy("sortOrder ASC, id ASC")
    @OneToMany(mappedBy = "mainTodo")
    private List<Todo> subTodos = new ArrayList<>();

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Convert(converter = RecurrenceRuleConverter.class)
    @Column(name = "recurrence_rule", columnDefinition = "json")
    private RecurrenceRule recurrenceRule;

    @Builder
    private Todo(
            Member member,
            Category category,
            Todo mainTodo,
            String title,
            String memo,
            TodoStatus status,
            Integer sortOrder,
            LocalDateTime dueAt,
            LocalDate scheduledDate,
            LocalTime scheduledTime,
            LocalDateTime completedAt,
            RecurrenceRule recurrenceRule
    ) {
        if (mainTodo != null && !mainTodo.isOwnedBy(member)) {
            throw new IllegalArgumentException(TodoError.MAIN_TODO_NOT_OWNED.message());
        }
        validateStatus(status);
        validateRecurrenceSchedule(recurrenceRule, scheduledDate);
        this.member = member;
        this.category = category;
        this.mainTodo = mainTodo;
        this.title = title;
        this.memo = memo;
        this.status = status;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.dueAt = dueAt;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.completedAt = completedAt;
        this.recurrenceRule = recurrenceRule;
    }

    public Long getCategoryId() {
        return category.getId();
    }

    public Long getMainTodoId() {
        return mainTodo != null ? mainTodo.getId() : null;
    }

    public boolean isOwnedBy(Member member) {
        if (member == null || this.member == null) {
            return false;
        }
        if (this.member == member) {
            return true;
        }

        return this.member.getId() != null && this.member.getId().equals(member.getId());
    }

    public boolean hasMainTodo() {
        return mainTodo != null;
    }

    public List<Todo> getSubTodos() {
        return Collections.unmodifiableList(subTodos);
    }

    public boolean isRecurringTodo() {
        return recurrenceRule != null;
    }

    /**
     * 완료 처리한다.
     * 반복 Todo는 다음 반복일이 있으면 scheduledDate를 이동하고, 없으면 DONE으로 변경한다.
     * 반복 mainTodo의 다음 회차가 있으면 subTodo를 TODO로 초기화하고, 영구 완료되면 함께 DONE으로 변경한다.
     */
    public void complete(LocalDateTime completedAt) {
        if (status == TodoStatus.DONE) {
            throw new IllegalStateException(TodoError.TODO_ALREADY_COMPLETED.message());
        }
        if (completedAt == null) {
            throw new IllegalArgumentException(TodoError.COMPLETED_DATE_REQUIRED.message());
        }

        Optional<LocalDate> nextDateOpt = nextDateAfterCompletion(completedAt);
        nextDateOpt.ifPresentOrElse(
                nextDate -> {
                    scheduledDate = nextDate;
                    resetSubTodos();
                },
                () -> {
                    setStatus(TodoStatus.DONE);
                    doneSubTodos(completedAt);
                }
        );

        this.completedAt = completedAt;
    }

    private Optional<LocalDate> nextDateAfterCompletion(LocalDateTime completedAt) {
        if (!isRecurringTodo()) {
            return Optional.empty();
        }

        return recurrenceRule.nextDate(scheduledDate, completedAt.toLocalDate());
    }


    /**
     * 완료를 취소한다.
     * mainTodo를 복구하면 subTodo도 함께 TODO로 복구하고, subTodo를 복구하면 mainTodo도 TODO로 복구한다.
     */
    public void undo() {
        if (status != TodoStatus.DONE) {
            throw new IllegalStateException(TodoError.TODO_NOT_COMPLETED.message());
        }

        setStatus(TodoStatus.TODO);
        completedAt = null;
        if (hasMainTodo()) {
            mainTodo.setStatus(TodoStatus.TODO);
            mainTodo.completedAt = null;
            return;
        }

        resetSubTodos();
    }

    private void setStatus(TodoStatus todoStatus) {
        this.status = todoStatus;
    }

    private void doneSubTodos(LocalDateTime completedAt) {
        subTodos.forEach(subTodo -> {
            subTodo.setStatus(TodoStatus.DONE);
            subTodo.completedAt = completedAt;
        });
    }

    private void resetSubTodos() {
        subTodos.forEach(subTodo -> {
            subTodo.setStatus(TodoStatus.TODO);
            subTodo.completedAt = null;
        });
    }

    private void validateStatus(TodoStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(TodoError.TODO_STATUS_REQUIRED.message());
        }
    }

    private void validateRecurrenceSchedule(RecurrenceRule recurrenceRule, LocalDate scheduledDate) {
        if (recurrenceRule != null && scheduledDate == null) {
            throw new IllegalArgumentException(TodoError.RECURRING_TODO_SCHEDULED_DATE_REQUIRED.message());
        }
    }
}
