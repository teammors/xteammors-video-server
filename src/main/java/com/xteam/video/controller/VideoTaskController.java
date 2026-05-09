package com.xteam.video.controller;

import com.xteam.video.config.ThirdPartyServiceConfig;
import com.xteam.video.dto.request.VideoTaskCreateRequest;
import com.xteam.video.dto.response.ApiResponse;
import com.xteam.video.dto.response.VideoTaskResponse;
import com.xteam.video.entity.VideoTask;
import com.xteam.video.enums.TaskStatus;
import com.xteam.video.service.VideoTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/video")
public class VideoTaskController {

    private static final Logger log = LoggerFactory.getLogger(VideoTaskController.class);

    private final VideoTaskService videoTaskService;
    private final ThirdPartyServiceConfig serviceConfig;

    public VideoTaskController(VideoTaskService videoTaskService, ThirdPartyServiceConfig serviceConfig) {
        this.videoTaskService = videoTaskService;
        this.serviceConfig = serviceConfig;
    }

    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VideoTaskResponse> createTask(@RequestBody VideoTaskCreateRequest request) {
        log.info("Received create task request: userId={}, fileUrls={}, service={}, generateAudio={}, videoRatio={}, videoDuration={}, showWatermark={}, resolution={}", 
                 request.getUserId(), request.getFileUrls(), request.getService(), 
                 request.getGenerateAudio(), request.getVideoRatio(), request.getVideoDuration(), 
                 request.getShowWatermark(), request.getResolution());

        VideoTask task = videoTaskService.createTask(
                request.getUserId(),
                request.getFileUrls(),
                request.getTextContent(),
                request.getService(),
                request.getGenerateAudio(),
                request.getVideoRatio(),
                request.getVideoDuration(),
                request.getShowWatermark(),
                request.getResolution()
        );
        return ApiResponse.success("任务创建成功", convertToResponse(task));
    }

    @GetMapping("/tasks/{id}")
    public ApiResponse<VideoTaskResponse> getTaskById(@PathVariable Long id) {
        VideoTask task = videoTaskService.getTaskById(id);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        return ApiResponse.success(convertToResponse(task));
    }

    @GetMapping("/users/{userId}/tasks")
    public ApiResponse<List<VideoTaskResponse>> getTasksByUserId(@PathVariable String userId) {
        List<VideoTask> tasks = videoTaskService.getTasksByUserId(userId);
        List<VideoTaskResponse> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/services")
    public ApiResponse<List<ThirdPartyServiceConfig.ServiceOption>> getAvailableServices() {
        return ApiResponse.success(serviceConfig.getServiceOptions());
    }

    private VideoTaskResponse convertToResponse(VideoTask task) {
        VideoTaskResponse response = new VideoTaskResponse();
        response.setId(task.getId());
        response.setUserId(task.getUserId());
        response.setFilePath(task.getFilePath());
        response.setFileType(task.getFileType());
        response.setTextContent(task.getTextContent());
        response.setThirdPartyService(task.getThirdPartyService());
        response.setStatus(task.getStatus());
        response.setStatusDesc(TaskStatus.fromCode(task.getStatus()).getDesc());
        response.setResultUrl(task.getResultUrl());
        response.setErrorMessage(task.getErrorMessage());
        response.setRetryCount(task.getRetryCount());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setGenerateAudio(task.getGenerateAudio());
        response.setVideoRatio(task.getVideoRatio());
        response.setVideoDuration(task.getVideoDuration());
        response.setShowWatermark(task.getShowWatermark());
        response.setResolution(task.getResolution());
        return response;
    }
}