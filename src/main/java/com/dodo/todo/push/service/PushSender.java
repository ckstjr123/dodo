package com.dodo.todo.push.service;

public interface PushSender {

    void send(String fcmToken, String title, String body);
}
