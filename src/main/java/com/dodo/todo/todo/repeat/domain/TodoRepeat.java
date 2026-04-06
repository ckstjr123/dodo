package com.dodo.todo.todo.repeat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "todo_repeat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoRepeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "todo_id", nullable = false)
    private Long todoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 20)
    private TodoRepeatType repeatType;

    @Column(name = "repeat_interval", nullable = false)
    private int repeatInterval;

    /** 매주 반복 */
    @Convert(converter = DayOfWeekSetJsonConverter.class)
    @Column(name = "days_of_week_json", columnDefinition = "json")
    private Set<DayOfWeek> daysOfWeek = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private TodoRepeat(Long todoId, TodoRepeatType repeatType, int repeatInterval, Set<DayOfWeek> daysOfWeek) {
        validateTodoId(todoId);
        validateRepeatInterval(repeatInterval);
        this.todoId = todoId;
        this.repeatType = repeatType;
        this.repeatInterval = repeatInterval;
        this.daysOfWeek = sanitizeDaysOfWeek(daysOfWeek);
        validateDaysOfWeek(this.daysOfWeek);
    }

    public static TodoRepeat daily(Long todoId, int repeatInterval) {
        return new TodoRepeat(todoId, TodoRepeatType.DAILY, repeatInterval, Set.of());
    }

    public static TodoRepeat weekly(Long todoId, int repeatInterval, Set<DayOfWeek> daysOfWeek) {
        return new TodoRepeat(todoId, TodoRepeatType.WEEKLY, repeatInterval, daysOfWeek);
    }

    public void updateDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        Set<DayOfWeek> sanitizedDaysOfWeek = sanitizeDaysOfWeek(daysOfWeek);
        validateDaysOfWeek(sanitizedDaysOfWeek);
        this.daysOfWeek = sanitizedDaysOfWeek;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return new LinkedHashSet<>(daysOfWeek);
    }

    private void validateTodoId(Long todoId) {
        if (todoId == null) {
            throw new IllegalArgumentException("Todo id is required");
        }
    }

    private void validateRepeatInterval(int repeatInterval) {
        if (repeatInterval < 1) {
            throw new IllegalArgumentException("Repeat interval must be at least 1");
        }
    }

    private Set<DayOfWeek> sanitizeDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        Set<DayOfWeek> sanitizedDaysOfWeek = new LinkedHashSet<>();

        if (daysOfWeek == null) {
            return sanitizedDaysOfWeek;
        }

        daysOfWeek.stream()
                .filter(Objects::nonNull)
                .sorted()
                .forEach(sanitizedDaysOfWeek::add);

        return sanitizedDaysOfWeek;
    }

    private void validateDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        // 주간 반복만 요일 목록을 가지며, 일간 반복은 요일 값을 비워둔다.
        if (repeatType == TodoRepeatType.DAILY && !daysOfWeek.isEmpty()) {
            throw new IllegalArgumentException("Daily repeat cannot have days of week");
        }

        if (repeatType == TodoRepeatType.WEEKLY && daysOfWeek.isEmpty()) {
            throw new IllegalArgumentException("Weekly repeat requires at least one day of week");
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
