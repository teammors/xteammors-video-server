package com.xteam.video.service.thirdparty;

public class GenerationResult {
    private final String videoUrl;
    private final String fullResponse;

    public GenerationResult(String videoUrl, String fullResponse) {
        this.videoUrl = videoUrl;
        this.fullResponse = fullResponse;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getFullResponse() {
        return fullResponse;
    }
}
