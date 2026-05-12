package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrence;
import com.dodo.todo.todo.domain.recurrence.TodoRecurrenceConverter;
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
import java.util.Objects;
import java.util.Optional;
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
    @JoinColumn(name = "parent_id")
    private Todo mainTodo;

    @BatchSize(size = 100)
    @OrderBy("sortOrder ASC, id ASC")
    @OneToMany(mappedBy = "mainTodo")
    private List<Todo> subTodos = new ArrayList<>();

    @BatchSize(size = 100)
    @OrderBy("remindAt ASC, id ASC")
    @OneToMany(mappedBy = "todo")
    private List<Reminder> reminders = new ArrayList<>();

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

    @Convert(converter = TodoRecurrenceConverter.class)
    @Column(name = "recurrence", columnDefinition = "json")
    private TodoRecurrence recurrence;

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
            TodoRecurrence recurrence
    ) {
        if (mainTodo != null && !mainTodo.isOwnedBy(member)) {
            throw new BusinessException(TodoError.MAIN_TODO_NOT_OWNED);
        }
        if (status == null) {
            throw new BusinessException(TodoError.TODO_STATUS_REQUIRED);
        }
        validateRecurrenceSchedule(recurrence, scheduledDate);
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
        this.recurrence = recurrence;
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

    public List<Reminder> getReminders() {
        return Collections.unmodifiableList(reminders);
    }

    public boolean isRecurringTodo() {
        return recurrence != null;
    }

    /**
     * Todo 기본 정보 수정
     * 완료 상태와 완료 시각은 완료/취소 기능에서만 변경한다.
     */
    public void updateDetails(
            Category category,
            String title,
            String memo,
            Integer sortOrder,
            LocalDateTime dueAt,
            LocalDate scheduledDate,
            LocalTime scheduledTime,
            TodoRecurrence recurrence
    ) {
        this.category = category;
        this.title = title;
        this.memo = memo;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.dueAt = dueAt;

        updateSchedule(scheduledDate, scheduledTime, recurrence);
    }

    private void updateSchedule(
            LocalDate scheduledDate,
            LocalTime scheduledTime,
            TodoRecurrence recurrence
    ) {
        if (scheduledDate == null) {
            scheduledTime = null;
            recurrence = null;
        }
        validateRecurrenceSchedule(recurrence, scheduledDate);

        boolean isScheduleChanged = !Objects.equals(this.scheduledDate, scheduledDate)
                || !Objects.equals(this.scheduledTime, scheduledTime);

        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.recurrence = recurrence;

        if (isScheduleChanged && scheduledDate != null && scheduledTime != null) {
            rescheduleReminders();
        }
    }

    /**
     * 완료 처리
     * 반복 Todo에 다음 회차가 있으면 scheduledDate를 이동하고, 없으면 DONE으로 변경한다.
     * 반복 mainTodo에 다음 회차가 있으면 subTodo를 TODO로 초기화하고, 영구 완료되면 함께 DONE으로 변경한다.
     */
    public void complete(LocalDateTime completedAt) {
        if (status == TodoStatus.DONE) {
            throw new BusinessException(TodoError.TODO_ALREADY_COMPLETED);
        }
        if (completedAt == null) {
            throw new BusinessException(TodoError.COMPLETED_DATE_REQUIRED);
        }

        nextDate(completedAt).ifPresentOrElse(
                nextDate -> {
                    scheduledDate = nextDate;
                    rescheduleReminders();
                    resetSubTodos();
                },
                () -> {
                    setStatus(TodoStatus.DONE);
                    completeSubTodos(completedAt);
                }
        );
        this.completedAt = completedAt;
    }

    private Optional<LocalDate> nextDate(LocalDateTime completedAt) {
        return this.isRecurringTodo()
                ? recurrence.nextDate(scheduledDate, completedAt.toLocalDate())
                : Optional.empty();
    }

    /**
     * 완료 취소
     * mainTodo를 복구하면 subTodo도 함께 TODO로 복구하고, subTodo를 복구하면 mainTodo를 TODO로 복구한다.
     */
    public void undo() {
        if (status != TodoStatus.DONE) {
            throw new BusinessException(TodoError.TODO_NOT_COMPLETED);
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

    private void completeSubTodos(LocalDateTime completedAt) {
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

    private void rescheduleReminders() {
        reminders.forEach(Reminder::reschedule);
    }

    private void validateRecurrenceSchedule(TodoRecurrence recurrence, LocalDate scheduledDate) {
        if (recurrence != null && scheduledDate == null) {
            throw new BusinessException(TodoError.RECURRING_TODO_SCHEDULED_DATE_REQUIRED);
        }
    }

}
