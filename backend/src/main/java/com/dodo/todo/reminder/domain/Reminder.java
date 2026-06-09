package com.dodo.todo.reminder.domain;

import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
import com.dodo.todo.todo.domain.Todo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reminder")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "reminder_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Reminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected Reminder(Todo todo, Member member) {
        validateSchedule(todo);
        this.todo = todo;
        this.member = member;
    }

    protected void validateSchedule(Todo todo) {
        if (!todo.hasScheduledDate() || !todo.hasScheduledTime()) {
            throw new BusinessException(ReminderError.REMINDER_SCHEDULE_REQUIRED);
        }
    }

    /**
     * 알림 타입 조회
     * 하위 엔티티의 discriminator 값과 API 타입을 일치시킴.
     */
    public abstract ReminderType getType();

    /**
     * 알림 수정
     * 기존 알림 row의 타입은 유지하고 타입에 맞는 설정만 변경함.
     */
    public abstract void update(ReminderUpdateRequest request);

    /**
     * 알림 발송 시각 계산
     * DB에 저장하지 않고 타입별 기준 값으로 필요 시 계산함.
     */
    public abstract LocalDateTime calculateRemindAt();
}
