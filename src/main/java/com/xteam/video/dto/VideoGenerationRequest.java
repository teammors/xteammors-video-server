package com.xteam.video.dto;

import java.util.ArrayList;
import java.util.List;

public class VideoGenerationRequest {

    private String textContent;
    private List<ResourceItem> resources = new ArrayList<>();
    private Boolean generateAudio = true;
    private String videoRatio = "16:9";
    private Long videoDuration = 11L;
    private Boolean showWatermark = true;
    private String resolution = "720p";

    public VideoGenerationRequest() {}

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public List<ResourceItem> getResources() {
        return resources;
    }

    public void setResources(List<ResourceItem> resources) {
        this.resources = resources;
    }

    public void addResource(ResourceItem item) {
        this.resources.add(item);
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

    public static class ResourceItem {
        private String url;
        private ResourceType type;

        public ResourceItem() {}

        public ResourceItem(String url, ResourceType type) {
            this.url = url;
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public ResourceType getType() {
            return type;
        }

        public void setType(ResourceType type) {
            this.type = type;
        }

        public static ResourceItem image(String url) {
            return new ResourceItem(url, ResourceType.IMAGE);
        }

        public static ResourceItem video(String url) {
            return new ResourceItem(url, ResourceType.VIDEO);
        }

        public static ResourceItem audio(String url) {
            return new ResourceItem(url, ResourceType.AUDIO);
        }
    }

    public enum ResourceType {
        IMAGE, VIDEO, AUDIO
    }
}