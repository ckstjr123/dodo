package com.dodo.todo.reminder.controller;

import com.dodo.todo.reminder.dto.ReminderCreateRequest;
import com.dodo.todo.reminder.dto.ReminderCreateResponse;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reminder", description = "Reminder API")
public interface ReminderApiDocs {

    @Operation(summary = "알림 생성")
    @SecurityRequirement(name = "bearerAuth")
    ReminderCreateResponse createReminder(Long memberId, ReminderCreateRequest request);

    @Operation(summary = "알림 수정")
    @SecurityRequirement(name = "bearerAuth")
    void updateReminder(Long memberId, Long reminderId, ReminderUpdateRequest request);

    @Operation(summary = "알림 삭제")
    @SecurityRequirement(name = "bearerAuth")
    void deleteReminder(Long memberId, Long reminderId);
}
