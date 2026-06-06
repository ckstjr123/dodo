package com.dodo.todo.reminder.domain;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
import com.dodo.todo.todo.domain.Todo;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@DiscriminatorValue("RELATIVE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RelativeReminder extends Reminder {

    @Column(name = "minute_offset")
    private int minuteOffset;

    private RelativeReminder(Todo todo, Member member, int minuteOffset) {
        super(todo, member);
        validateMinuteOffset(minuteOffset);
        this.minuteOffset = minuteOffset;
    }

    public static RelativeReminder create(Todo todo, Member member, int minuteOffset) {
        return new RelativeReminder(todo, member, minuteOffset);
    }

    @Override
    public ReminderType getType() {
        return ReminderType.RELATIVE;
    }

    @Override
    public void update(ReminderUpdateRequest request) {
        if (request.minuteOffset() != null) {
            validateMinuteOffset(request.minuteOffset());
            this.minuteOffset = request.minuteOffset();
        }
    }

    @Override
    public LocalDateTime calculateRemindAt() {
        validateSchedule(getTodo());
        return LocalDateTime.of(getTodo().getScheduledDate(), getTodo().getScheduledTime())
                .minusMinutes(minuteOffset);
    }

    private void validateMinuteOffset(int minuteOffset) {
        if (minuteOffset < 0) {
            throw new BusinessException(ReminderError.REMINDER_OFFSET_NEGATIVE);
        }
    }
}
