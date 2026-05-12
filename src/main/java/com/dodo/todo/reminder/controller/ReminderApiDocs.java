package com.dodo.todo.reminder.controller;

import com.dodo.todo.reminder.dto.ReminderRequest;
import com.dodo.todo.reminder.dto.ReminderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reminder", description = "Reminder API")
public interface ReminderApiDocs {

    @Operation(summary = "알림 생성")
    @SecurityRequirement(name = "bearerAuth")
    ReminderResponse createReminder(Long memberId, Long todoId, ReminderRequest request);

    @Operation(summary = "알림 수정")
    @SecurityRequirement(name = "bearerAuth")
    ReminderResponse updateReminder(Long memberId, Long todoId, Long reminderId, ReminderRequest request);

    @Operation(summary = "알림 삭제")
    @SecurityRequirement(name = "bearerAuth")
    void deleteReminder(Long memberId, Long todoId, Long reminderId);
}
