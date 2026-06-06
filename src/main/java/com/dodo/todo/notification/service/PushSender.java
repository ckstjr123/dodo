package com.dodo.todo.notification.service;

public interface PushSender {

    void send(String fcmToken, String title, String body);
}
