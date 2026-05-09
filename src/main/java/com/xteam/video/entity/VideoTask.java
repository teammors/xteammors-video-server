package com.xteam.video.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xteam.video.enums.TaskStatus;

import java.time.LocalDateTime;

@TableName("video_task")
public class VideoTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("file_path")
    private String filePath;

    @TableField("file_type")
    private String fileType;

    @TableField("text_content")
    private String textContent;

    @TableField("third_party_service")
    private String thirdPartyService;

    @TableField("status")
    private String status;

    @TableField("result_url")
    private String resultUrl;

    @TableField("error_message")
    private String errorMessage;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("generate_audio")
    private Boolean generateAudio = true;

    @TableField("video_ratio")
    private String videoRatio = "16:9";

    @TableField("video_duration")
    private Long videoDuration = 11L;

    @TableField("show_watermark")
    private Boolean showWatermark = true;

    @TableField("resolution")
    private String resolution = "720p";

    @TableField("model_task_id")
    private String modelTaskId;

    @TableField("completed_response")
    private String completedResponse;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getThirdPartyService() {
        return thirdPartyService;
    }

    public void setThirdPartyService(String thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getGenerateAudio() {
        return generateAudio;
    }

    public void setGenerateAudio(Boolean generateAudio) {
        this.generateAudio = generateAudio;
    }

    public String getVideoRatio() {
        return videoRatio;
    }

    public void setVideoRatio(String videoRatio) {
        this.videoRatio = videoRatio;
    }

    public Long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Long videoDuration) {
        this.videoDuration = videoDuration;
    }

    public Boolean getShowWatermark() {
        return showWatermark;
    }

    public void setShowWatermark(Boolean showWatermark) {
        this.showWatermark = showWatermark;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getModelTaskId() {
        return modelTaskId;
    }

    public void setModelTaskId(String modelTaskId) {
        this.modelTaskId = modelTaskId;
    }

    public String getCompletedResponse() {
        return completedResponse;
    }

    public void setCompletedResponse(String completedResponse) {
        this.completedResponse = completedResponse;
    }

    public TaskStatus getStatusEnum() {
        return TaskStatus.fromCode(status);
    }

    public void setStatusEnum(TaskStatus taskStatus) {
        this.status = taskStatus.getCode();
    }
}