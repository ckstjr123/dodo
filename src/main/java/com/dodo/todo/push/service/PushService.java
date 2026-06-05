package com.dodo.todo.push.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PushService {

    private final PushSender pushSender;

    /**
     * 푸시 알림 발송
     * FCM token이 있는 대상에게 제목과 본문을 전달한다.
     */
    public void send(String fcmToken, String title, String body) {
        if (!StringUtils.hasText(fcmToken)) {
            return;
        }

        pushSender.send(fcmToken, title, body);
    }
}
