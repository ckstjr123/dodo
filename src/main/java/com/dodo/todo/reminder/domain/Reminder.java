package com.dodo.todo.reminder.domain;

import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reminder")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Column(name = "minute_offset", nullable = false)
    private int minuteOffset;

    private Reminder(Todo todo, Member member, int minuteOffset) {
        validateSchedule(todo.getScheduledDate(), todo.getScheduledTime());
        validateMinuteOffset(minuteOffset);
        this.todo = todo;
        this.member = member;
        this.minuteOffset = minuteOffset;
        this.remindAt = calculateRemindAt(todo.getScheduledDate(), todo.getScheduledTime(), minuteOffset);
    }

    /**
     * 알림 생성
     * Todo 일정 기준 시각에서 minuteOffset만큼 앞선 알림 시각을 계산한다.
     */
    public static Reminder create(Todo todo, Member member, int minuteOffset) {
        return new Reminder(todo, member, minuteOffset);
    }

    /**
     * 알림 offset 수정
     * 기존 알림 row를 유지하면서 Todo 일정 기준 알림 시각을 다시 계산한다.
     */
    public void updateMinuteOffset(int minuteOffset) {
        validateMinuteOffset(minuteOffset);
        this.minuteOffset = minuteOffset;
        reschedule();
    }

    /**
     * 알림 시각 재계산
     * Todo 일정이 변경되거나 반복 Todo가 다음 회차로 이동하면 기존 offset을 유지한다.
     */
    public void reschedule() {
        validateSchedule(todo.getScheduledDate(), todo.getScheduledTime());
        this.remindAt = calculateRemindAt(todo.getScheduledDate(), todo.getScheduledTime(), minuteOffset);
    }

    private LocalDateTime calculateRemindAt(LocalDate scheduledDate, LocalTime scheduledTime, int minuteOffset) {
        return LocalDateTime.of(scheduledDate, scheduledTime).minusMinutes(minuteOffset);
    }

    private void validateSchedule(LocalDate scheduledDate, LocalTime scheduledTime) {
        if (scheduledDate == null || scheduledTime == null) {
            throw new BusinessException(ReminderError.REMINDER_SCHEDULE_REQUIRED);
        }
    }

    private void validateMinuteOffset(int minuteOffset) {
        if (minuteOffset < 0) {
            throw new BusinessException(ReminderError.REMINDER_OFFSET_NEGATIVE);
        }
    }
}
