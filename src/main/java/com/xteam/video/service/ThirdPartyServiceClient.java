package com.xteam.video.service;

import com.xteam.video.dto.VideoGenerationRequest;
import com.xteam.video.service.thirdparty.GenerationResult;

public interface ThirdPartyServiceClient {

    String generateVideo(String filePath, String textContent, String serviceKey);

    String generateVideo(VideoGenerationRequest request, String serviceKey);

    GenerationResult generateVideoWithResult(String filePath, String textContent, String serviceKey);

    GenerationResult generateVideoWithResult(VideoGenerationRequest request, String serviceKey);

    String getModelTaskId(String filePath, String textContent, String serviceKey);

    String getModelTaskId(VideoGenerationRequest request, String serviceKey);

    GenerationResult pollForExistingTask(String modelTaskId, String serviceKey);
}