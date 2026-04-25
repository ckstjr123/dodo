package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRule;
import com.dodo.todo.todo.domain.recurrence.RecurrenceRuleConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

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
            RecurrenceRule recurrenceRule
    ) {
        validateMember(member);
        validateCategory(category);
        validateStatus(status);
        if (mainTodo != null && !mainTodo.isOwnedBy(member)) {
            throw new IllegalArgumentException("Main todo must belong to the same member");
        }
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
        this.recurrenceRule = recurrenceRule;
    }

    public Long getCategoryId() {
        return category.getId();
    }

    public Long getMainTodoId() {
        return mainTodo == null ? null : mainTodo.getId();
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
     * mainTodo를 완료하면 subTodo도 함께 DONE으로 변경한다.
     */
    public void complete() {
        if (status == TodoStatus.DONE) {
            throw new IllegalStateException("Todo already completed");
        }

        if (isRecurringTodo()) {
            LocalDate nextScheduledDate = recurrenceRule.nextDate(scheduledDate);
            if (nextScheduledDate == null) {
                status = TodoStatus.DONE;
            } else {
                scheduledDate = nextScheduledDate;
            }
        } else {
            status = TodoStatus.DONE;
        }

        subTodos.forEach(subTodo -> subTodo.status = TodoStatus.DONE);
    }

    /**
     * 완료를 취소한다.
     * mainTodo를 복구하면 subTodo도 함께 TODO로 복구한다.
     */
    public void undo() {
        if (status != TodoStatus.DONE) {
            throw new IllegalStateException("Todo is not completed");
        }

        status = TodoStatus.TODO;
        subTodos.forEach(subTodo -> subTodo.status = TodoStatus.TODO);
    }

    private void validateMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member is required");
        }
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
    }

    private void validateStatus(TodoStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Todo status is required");
        }
    }

    private void validateRecurrenceSchedule(RecurrenceRule recurrenceRule, LocalDate scheduledDate) {
        if (recurrenceRule != null && scheduledDate == null) {
            throw new IllegalArgumentException("Scheduled date is required for recurring todo");
        }
    }
}
