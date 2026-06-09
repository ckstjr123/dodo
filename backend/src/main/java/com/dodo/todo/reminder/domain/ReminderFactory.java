package com.dodo.todo.reminder.domain;

import com.dodo.todo.member.domain.Member;
import com.dodo.todo.reminder.dto.ReminderCreateRequest;
import com.dodo.todo.todo.domain.Todo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReminderFactory {

    /**
     * 알림 생성
     * 요청 타입에 맞는 하위 알림 엔티티를 생성함.
     */
    public static Reminder create(Todo todo, Member member, ReminderCreateRequest request) {
        return switch (request.getReminderType()) {
            case RELATIVE -> RelativeReminder.create(todo, member, request.minuteOffset());
            case ABSOLUTE -> AbsoluteReminder.create(todo, member, request.due());
        };
    }
}
