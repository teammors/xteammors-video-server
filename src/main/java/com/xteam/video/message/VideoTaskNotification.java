package com.xteam.video.message;

import com.xteam.video.enums.TaskStatus;

import java.time.LocalDateTime;

public class VideoTaskNotification {

    private Long taskId;

    private String userId;

    private String taskStatus;

    private String statusDesc;

    private String resultUrl;

    private String errorMessage;

    private LocalDateTime completedAt;

    private String thirdPartyService;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getThirdPartyService() {
        return thirdPartyService;
    }

    public void setThirdPartyService(String thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    public static VideoTaskNotification success(Long taskId, String userId, String resultUrl, String thirdPartyService) {
        VideoTaskNotification notification = new VideoTaskNotification();
        notification.setTaskId(taskId);
        notification.setUserId(userId);
        notification.setTaskStatus(TaskStatus.COMPLETED.getCode());
        notification.setStatusDesc(TaskStatus.COMPLETED.getDesc());
        notification.setResultUrl(resultUrl);
        notification.setThirdPartyService(thirdPartyService);
        notification.setCompletedAt(LocalDateTime.now());
        return notification;
    }

    public static VideoTaskNotification failure(Long taskId, String userId, String errorMessage, String thirdPartyService) {
        VideoTaskNotification notification = new VideoTaskNotification();
        notification.setTaskId(taskId);
        notification.setUserId(userId);
        notification.setTaskStatus(TaskStatus.FAILED.getCode());
        notification.setStatusDesc(TaskStatus.FAILED.getDesc());
        notification.setErrorMessage(errorMessage);
        notification.setThirdPartyService(thirdPartyService);
        notification.setCompletedAt(LocalDateTime.now());
        return notification;
    }
}