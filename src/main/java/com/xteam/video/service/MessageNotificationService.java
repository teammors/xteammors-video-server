package com.xteam.video.service;

import com.xteam.video.message.VideoTaskNotification;

public interface MessageNotificationService {

    void sendNotification(VideoTaskNotification notification);
}