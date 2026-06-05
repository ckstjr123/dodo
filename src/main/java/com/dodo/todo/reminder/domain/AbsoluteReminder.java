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
@DiscriminatorValue("ABSOLUTE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbsoluteReminder extends Reminder {

    @Column(name = "due")
    private LocalDateTime due;

    private AbsoluteReminder(Todo todo, Member member, LocalDateTime due) {
        super(todo, member);
        validateDue(due);
        this.due = due;
    }

    public static AbsoluteReminder create(Todo todo, Member member, LocalDateTime due) {
        return new AbsoluteReminder(todo, member, due);
    }

    @Override
    public ReminderType getType() {
        return ReminderType.ABSOLUTE;
    }

    @Override
    public void update(ReminderUpdateRequest request) {
        if (request.due() != null) {
            validateDue(request.due());
            this.due = request.due();
        }
    }

    @Override
    public LocalDateTime calculateRemindAt() {
        return due;
    }

    private static void validateDue(LocalDateTime due) {
        if (due == null) {
            throw new BusinessException(ReminderError.REMINDER_DUE_REQUIRED);
        }
    }
}
