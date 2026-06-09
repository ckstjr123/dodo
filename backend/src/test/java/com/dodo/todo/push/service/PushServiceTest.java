package com.dodo.todo.push.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dodo.todo.notification.service.PushSender;
import com.dodo.todo.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    @Mock
    private PushSender pushSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("FCM token이 있으면 푸시 알림을 발송한다")
    void sendPush() {
        notificationService.send("fcm-token", "title", "body");

        verify(pushSender).send("fcm-token", "title", "body");
    }

    @Test
    @DisplayName("FCM token이 없으면 푸시 알림을 발송하지 않는다")
    void skipBlankFcmToken() {
        notificationService.send("", "title", "body");

        verify(pushSender, never()).send("", "title", "body");
    }
}
