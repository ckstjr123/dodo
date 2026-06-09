package com.dodo.todo.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(FirebaseMessaging.class)
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * FCM 메시지 발송
     * 지정된 FCM token으로 알림 제목과 본문을 전송한다.
     */
    @Override
    public void send(String fcmToken, String title, String body) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException exception) {
            throw new IllegalStateException("Failed to send FCM message", exception);
        }
    }
}
