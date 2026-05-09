package com.xteam.video.dto.request;

import java.util.List;

public class VideoTaskCreateRequest {

    private String userId;

    private List<String> fileUrls;

    private String textContent;

    private String service;

    private Boolean generateAudio = true;

    private String videoRatio = "16:9";

    private Long videoDuration = 11L;

    private Boolean showWatermark = true;

    private String resolution = "720p";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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
}