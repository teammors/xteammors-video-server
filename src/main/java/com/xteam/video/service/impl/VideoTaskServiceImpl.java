package com.xteam.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xteam.video.entity.VideoTask;
import com.xteam.video.enums.FileType;
import com.xteam.video.enums.TaskStatus;
import com.xteam.video.mapper.VideoTaskMapper;
import com.xteam.video.service.VideoTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VideoTaskServiceImpl extends ServiceImpl<VideoTaskMapper, VideoTask> implements VideoTaskService {

    private static final Logger log = LoggerFactory.getLogger(VideoTaskServiceImpl.class);

    private final VideoTaskMapper videoTaskMapper;

    public VideoTaskServiceImpl(VideoTaskMapper videoTaskMapper) {
        this.videoTaskMapper = videoTaskMapper;
    }

    @Override
    public VideoTask createTask(String userId, List<String> fileUrls, String textContent, String service,
                                Boolean generateAudio, String videoRatio, Long videoDuration, Boolean showWatermark, String resolution) {

        String fileExtension = null;
        String fileType = null;
        String storedFilePath = null;

        if (fileUrls != null && !fileUrls.isEmpty()) {
            String firstUrl = fileUrls.get(0);
            fileExtension = getFileExtensionFromUrl(firstUrl);
            fileType = determineFileType(fileExtension);
            storedFilePath = firstUrl;

            if (fileUrls.size() > 1) {
                log.info("Multiple files provided, only processing first one: {}", firstUrl);
            }
        } else {
            throw new IllegalArgumentException("必须提供 fileUrls 参数");
        }

        VideoTask task = new VideoTask();
        task.setUserId(userId);
        task.setFilePath(storedFilePath);
        task.setFileType(fileType);
        task.setTextContent(textContent);
        task.setThirdPartyService(service);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setGenerateAudio(generateAudio != null ? generateAudio : true);
        task.setVideoRatio(videoRatio != null ? videoRatio : "16:9");
        task.setVideoDuration(videoDuration != null ? videoDuration : 11L);
        task.setShowWatermark(showWatermark != null ? showWatermark : true);
        task.setResolution(resolution != null ? resolution : "720p");

        save(task);
        log.info("Created video task: userId={}, taskId={}, service={}, fileUrls={}, generateAudio={}, videoRatio={}, videoDuration={}, showWatermark={}, resolution={}", 
                 userId, task.getId(), service, fileUrls, generateAudio, videoRatio, videoDuration, showWatermark, resolution);
        return task;
    }

    @Override
    public VideoTask getTaskById(Long id) {
        return getById(id);
    }

    @Override
    public List<VideoTask> getTasksByUserId(String userId) {
        LambdaQueryWrapper<VideoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoTask::getUserId, userId)
                .orderByDesc(VideoTask::getCreatedAt);
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public boolean updateTaskStatus(Long id, String status, String resultUrl, String errorMessage) {
        VideoTask task = getById(id);
        if (task == null) {
            return false;
        }

        task.setStatus(status);
        task.setResultUrl(resultUrl);
        task.setErrorMessage(errorMessage);
        task.setUpdatedAt(LocalDateTime.now());

        if (TaskStatus.FAILED.getCode().equals(status)) {
            task.setRetryCount(task.getRetryCount() + 1);
        }

        return updateById(task);
    }

    @Override
    @Transactional
    public boolean updateTaskWithResult(Long id, String status, String resultUrl, String errorMessage, String completedResponse) {
        VideoTask task = getById(id);
        if (task == null) {
            return false;
        }

        task.setStatus(status);
        task.setResultUrl(resultUrl);
        task.setErrorMessage(errorMessage);
        task.setCompletedResponse(completedResponse);
        task.setUpdatedAt(LocalDateTime.now());

        if (TaskStatus.FAILED.getCode().equals(status)) {
            task.setRetryCount(task.getRetryCount() + 1);
        }

        return updateById(task);
    }

    @Override
    @Transactional
    public boolean updateModelTaskId(Long id, String modelTaskId) {
        VideoTask task = getById(id);
        if (task == null) {
            return false;
        }

        task.setModelTaskId(modelTaskId);
        task.setUpdatedAt(LocalDateTime.now());
        return updateById(task);
    }

    @Override
    public List<VideoTask> getPendingTasks() {
        return videoTaskMapper.selectPendingTasks();
    }

    @Override
    public List<VideoTask> getProcessingTasks() {
        LambdaQueryWrapper<VideoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoTask::getStatus, TaskStatus.PROCESSING.getCode())
                .orderByDesc(VideoTask::getCreatedAt);
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public boolean lockAndProcessTask(Long id) {
        VideoTask task = videoTaskMapper.selectByIdWithLock(id);
        if (task == null) {
            return false;
        }

        if (!TaskStatus.PENDING.getCode().equals(task.getStatus()) && 
            !TaskStatus.PROCESSING.getCode().equals(task.getStatus())) {
            return false;
        }

        task.setStatus(TaskStatus.PROCESSING.getCode());
        task.setUpdatedAt(LocalDateTime.now());
        updateById(task);
        return true;
    }

    private String getFileExtensionFromUrl(String url) {
        if (url == null || !url.contains(".")) {
            return "tmp";
        }
        String path = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        return path.substring(path.lastIndexOf(".") + 1).toLowerCase();
    }

    private String determineFileType(String extension) {
        List<String> imageExtensions = List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
        List<String> videoExtensions = List.of("mp4", "mov", "avi", "mkv", "webm", "flv");

        if (imageExtensions.contains(extension)) {
            return FileType.IMAGE.getCode();
        } else if (videoExtensions.contains(extension)) {
            return FileType.VIDEO.getCode();
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }
}