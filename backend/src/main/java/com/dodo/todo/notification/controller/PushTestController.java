package com.dodo.todo.notification.controller;

import com.dodo.todo.notification.dto.PushTestRequest;
import com.dodo.todo.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushTestController {

    private final NotificationService notificationService;

    /**
     * 임시 푸시 알림 발송
     * 전달받은 FCM token으로 테스트용 알림을 즉시 발송한다.
     */
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendTestPush(@Valid @RequestBody PushTestRequest request) {
        notificationService.send(request.fcmToken(), request.title(), request.body());
    }
}
