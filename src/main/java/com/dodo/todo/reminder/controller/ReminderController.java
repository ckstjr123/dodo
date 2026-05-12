package com.dodo.todo.reminder.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.reminder.dto.ReminderRequest;
import com.dodo.todo.reminder.dto.ReminderResponse;
import com.dodo.todo.reminder.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todos/{todoId}/reminders")
@RequiredArgsConstructor
public class ReminderController implements ReminderApiDocs {

    private final ReminderService reminderService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse createReminder(@LoginMember Long memberId,
                                           @PathVariable Long todoId,
                                           @Valid @RequestBody ReminderRequest request) {
        return reminderService.createReminder(memberId, todoId, request);
    }

    @Override
    @PatchMapping("/{reminderId}")
    public ReminderResponse updateReminder(@LoginMember Long memberId,
                                           @PathVariable Long todoId,
                                           @PathVariable Long reminderId,
                                           @Valid @RequestBody ReminderRequest request) {
        return reminderService.updateReminder(memberId, todoId, reminderId, request);
    }

    @Override
    @DeleteMapping("/{reminderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReminder(@LoginMember Long memberId,
                               @PathVariable Long todoId,
                               @PathVariable Long reminderId) {
        reminderService.deleteReminder(memberId, todoId, reminderId);
    }
}
