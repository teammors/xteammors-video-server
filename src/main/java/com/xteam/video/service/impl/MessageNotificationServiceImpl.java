package com.xteam.video.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xteam.video.message.VideoTaskNotification;
import com.xteam.video.service.MessageNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageNotificationServiceImpl implements MessageNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MessageNotificationServiceImpl.class);

    private final ObjectMapper objectMapper;

    public MessageNotificationServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendNotification(VideoTaskNotification notification) {
        try {
            String notificationJson = objectMapper.writeValueAsString(notification);
            log.info("Sending notification to user {}: {}", notification.getUserId(), notificationJson);
        } catch (Exception e) {
            log.error("Failed to serialize notification: {}", e.getMessage(), e);
        }
    }
}