package com.dodo.todo.reminder.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.reminder.dto.ReminderCreateResponse;
import com.dodo.todo.reminder.dto.ReminderCreateRequest;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
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
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController implements ReminderApiDocs {

    private final ReminderService reminderService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderCreateResponse createReminder(@LoginMember Long memberId,
                                                 @Valid @RequestBody ReminderCreateRequest request) {
        return new ReminderCreateResponse(reminderService.saveReminder(memberId, request));
    }

    @Override
    @PatchMapping("/{reminderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateReminder(@LoginMember Long memberId,
                               @PathVariable Long reminderId,
                               @Valid @RequestBody ReminderUpdateRequest request) {
        reminderService.updateReminder(memberId, reminderId, request);
    }

    @Override
    @DeleteMapping("/{reminderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReminder(@LoginMember Long memberId,
                               @PathVariable Long reminderId) {
        reminderService.deleteReminder(memberId, reminderId);
    }
}
