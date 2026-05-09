package com.xteam.video.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xteam.video.entity.VideoTask;

import java.util.List;

public interface VideoTaskService extends IService<VideoTask> {

    VideoTask createTask(String userId, List<String> fileUrls, String textContent, String service,
                         Boolean generateAudio, String videoRatio, Long videoDuration, Boolean showWatermark, String resolution);

    VideoTask getTaskById(Long id);

    List<VideoTask> getTasksByUserId(String userId);

    boolean updateTaskStatus(Long id, String status, String resultUrl, String errorMessage);

    boolean updateTaskWithResult(Long id, String status, String resultUrl, String errorMessage, String completedResponse);

    boolean updateModelTaskId(Long id, String modelTaskId);

    List<VideoTask> getPendingTasks();

    List<VideoTask> getProcessingTasks();

    boolean lockAndProcessTask(Long id);
}