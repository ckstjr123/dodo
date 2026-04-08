package com.dodo.todo.todo.service;

import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.reminder.domain.ReminderType;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReminderPolicy {

    /**
     * 알림 추가
     * 알림 타입에 필요한 필드를 검증한 뒤 Todo에 알림 규칙을 등록함.
     */
    public void addReminder(Todo todo, TodoCreateRequest.ReminderRequest request) {
        if (request.reminderType() == ReminderType.RELATIVE_TO_DUE) {
            addRelativeReminder(todo, request.remindBefore());
            return;
        }

        addAbsoluteReminder(todo, request.remindAt());
    }

    private void addRelativeReminder(Todo todo, Integer remindBefore) {
        if (todo.getDueAt() == null) {
            throw new ApiException("DUE_AT_REQUIRED", HttpStatus.BAD_REQUEST, "Due date is required for relative reminder");
        }

        if (remindBefore == null) {
            throw new ApiException("REMIND_BEFORE_REQUIRED", HttpStatus.BAD_REQUEST, "Remind before is required");
        }

        if (todo.hasRelativeReminder(remindBefore)) {
            throw new ApiException("DUPLICATE_REMINDER", HttpStatus.BAD_REQUEST, "Duplicate relative reminder");
        }

        todo.addRelativeReminder(remindBefore);
    }

    private void addAbsoluteReminder(Todo todo, LocalDateTime remindAt) {
        if (remindAt == null) {
            throw new ApiException("REMIND_AT_REQUIRED", HttpStatus.BAD_REQUEST, "Remind at is required");
        }

        if (todo.hasAbsoluteReminder(remindAt)) {
            throw new ApiException("DUPLICATE_REMINDER", HttpStatus.BAD_REQUEST, "Duplicate absolute reminder");
        }

        todo.addAbsoluteReminder(remindAt);
    }
}
