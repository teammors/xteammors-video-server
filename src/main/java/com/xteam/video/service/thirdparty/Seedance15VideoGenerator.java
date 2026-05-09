package com.xteam.video.service.thirdparty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xteam.video.config.ThirdPartyServiceConfig;
import com.xteam.video.dto.VideoGenerationRequest;
import com.xteam.video.dto.VideoGenerationRequest.ResourceItem;
import com.xteam.video.dto.VideoGenerationRequest.ResourceType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Seedance15VideoGenerator {

    private static final Logger log = LoggerFactory.getLogger(Seedance15VideoGenerator.class);
    private static final String SERVICE_KEY = "seedance-1-5-pro";

    private final ThirdPartyServiceConfig serviceConfig;
    private OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Seedance15VideoGenerator(ThirdPartyServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("Seedance15VideoGenerator initialized for model: {}", serviceConfig.getModelId(SERVICE_KEY));
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            log.info("Seedance15VideoGenerator shutdown");
        }
    }

    public GenerationResult generateVideo(String filePath, String textContent) {
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setTextContent(textContent);
        
        if (filePath != null && !filePath.isEmpty()) {
            String extension = getFileExtension(filePath).toLowerCase();
            if (isImageFile(extension)) {
                request.addResource(ResourceItem.image(filePath));
            } else if (isVideoFile(extension)) {
                request.addResource(ResourceItem.video(filePath));
            }
        }
        
        return generateVideo(request);
    }

    public GenerationResult generateVideo(VideoGenerationRequest request) {
        String modelId = serviceConfig.getModelId(SERVICE_KEY);
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        log.info("Starting video generation with Seedance 1.5 Pro: modelId={}, resolution={}", modelId, request.getResolution());

        try {
            Map<String, Object> createRequestBody = buildCreateRequestBody(request, modelId);
            String createRequestJson = objectMapper.writeValueAsString(createRequestBody);
            
            log.debug("Create request JSON: {}", createRequestJson);

            Request createHttpRequest = new Request.Builder()
                    .url(apiUrl + "/contents/generations/tasks")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(createRequestJson, MediaType.parse("application/json")))
                    .build();

            try (Response createResponse = client.newCall(createHttpRequest).execute()) {
                if (!createResponse.isSuccessful()) {
                    String errorBody = createResponse.body() != null ? createResponse.body().string() : "";
                    log.error("Create task failed: code={}, body={}", createResponse.code(), errorBody);
                    throw new RuntimeException("Create task failed: " + createResponse.code() + " - " + errorBody);
                }

                String createResponseBody = createResponse.body().string();
                log.info("Create task response: {}", createResponseBody);
                
                JsonNode createJsonNode = objectMapper.readTree(createResponseBody);
                String taskId = extractTaskId(createJsonNode);
                log.info("Task created successfully, taskId: {}", taskId);
                
                return pollTaskStatus(taskId);
            }

        } catch (Exception e) {
            log.error("Video generation failed with Seedance 1.5 Pro", e);
            throw new RuntimeException("Video generation failed: " + e.getMessage(), e);
        }
    }

    public String getModelTaskId(String filePath, String textContent) {
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setTextContent(textContent);
        
        if (filePath != null && !filePath.isEmpty()) {
            String extension = getFileExtension(filePath).toLowerCase();
            if (isImageFile(extension)) {
                request.addResource(ResourceItem.image(filePath));
            } else if (isVideoFile(extension)) {
                request.addResource(ResourceItem.video(filePath));
            }
        }
        
        return getModelTaskId(request);
    }

    public String getModelTaskId(VideoGenerationRequest request) {
        String modelId = serviceConfig.getModelId(SERVICE_KEY);
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        log.info("Creating video task with Seedance 1.5 Pro: modelId={}, resolution={}", modelId, request.getResolution());

        try {
            Map<String, Object> createRequestBody = buildCreateRequestBody(request, modelId);
            String createRequestJson = objectMapper.writeValueAsString(createRequestBody);
            
            log.debug("Create request JSON: {}", createRequestJson);

            Request createHttpRequest = new Request.Builder()
                    .url(apiUrl + "/contents/generations/tasks")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(createRequestJson, MediaType.parse("application/json")))
                    .build();

            try (Response createResponse = client.newCall(createHttpRequest).execute()) {
                if (!createResponse.isSuccessful()) {
                    String errorBody = createResponse.body() != null ? createResponse.body().string() : "";
                    log.error("Create task failed: code={}, body={}", createResponse.code(), errorBody);
                    throw new RuntimeException("Create task failed: " + createResponse.code() + " - " + errorBody);
                }

                String createResponseBody = createResponse.body().string();
                log.info("Create task response: {}", createResponseBody);
                
                JsonNode createJsonNode = objectMapper.readTree(createResponseBody);
                return extractTaskId(createJsonNode);
            }

        } catch (Exception e) {
            log.error("Create video task failed with Seedance 1.5 Pro", e);
            throw new RuntimeException("Create video task failed: " + e.getMessage(), e);
        }
    }

    public GenerationResult pollForExistingTask(String modelTaskId) {
        return pollTaskStatus(modelTaskId);
    }

    private Map<String, Object> buildCreateRequestBody(VideoGenerationRequest request, String modelId) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);

        List<Map<String, Object>> contentList = new ArrayList<>();

        if (request.getTextContent() != null && !request.getTextContent().isEmpty()) {
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            String fullText = buildFullTextContent(request);
            textContent.put("text", fullText);
            contentList.add(textContent);
            log.debug("Adding text content: {}", fullText);
        }

        if (request.getResources() != null && !request.getResources().isEmpty()) {
            for (ResourceItem resource : request.getResources()) {
                Map<String, Object> content = new HashMap<>();
                if (resource.getType() == ResourceType.IMAGE) {
                    content.put("type", "image_url");
                    Map<String, String> imageUrl = new HashMap<>();
                    imageUrl.put("url", resource.getUrl());
                    content.put("image_url", imageUrl);
                    log.debug("Adding reference image: {}", resource.getUrl());
                } else if (resource.getType() == ResourceType.VIDEO) {
                    content.put("type", "video_url");
                    Map<String, String> videoUrl = new HashMap<>();
                    videoUrl.put("url", resource.getUrl());
                    content.put("video_url", videoUrl);
                    log.debug("Adding reference video: {}", resource.getUrl());
                }
                contentList.add(content);
            }
        }

        requestBody.put("content", contentList);
        log.debug("Final request body: {}", requestBody);
        return requestBody;
    }

    private String buildFullTextContent(VideoGenerationRequest request) {
        StringBuilder sb = new StringBuilder();
        
        if (request.getTextContent() != null && !request.getTextContent().isEmpty()) {
            sb.append(request.getTextContent());
        }
        
        if (request.getVideoDuration() != null && request.getVideoDuration() > 0) {
            sb.append(" --duration ").append(request.getVideoDuration());
        }
        
        String resolutionDesc = getResolutionDescription(request.getResolution());
        if (resolutionDesc != null && !resolutionDesc.isEmpty()) {
            sb.append(" ").append(resolutionDesc);
        }
        
        if (request.getShowWatermark() != null) {
            sb.append(" --watermark ").append(request.getShowWatermark());
        }
        
        return sb.toString();
    }

    private String extractTaskId(JsonNode createResponse) {
        try {
            if (createResponse.has("id")) {
                return createResponse.get("id").asText();
            }
            if (createResponse.has("task_id")) {
                return createResponse.get("task_id").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract task ID from response: {}", e.getMessage());
        }
        throw new RuntimeException("Failed to extract task ID from response");
    }

    private GenerationResult pollTaskStatus(String taskId) {
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);
        
        int pollIntervalSeconds = 30;
        int maxPollRetries = 60;
        int retries = 0;
        
        try {
            while (retries < maxPollRetries) {
                Request getHttpRequest = new Request.Builder()
                        .url(apiUrl + "/contents/generations/tasks/" + taskId)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();

                try (Response getResponse = client.newCall(getHttpRequest).execute()) {
                    if (!getResponse.isSuccessful()) {
                        String errorBody = getResponse.body() != null ? getResponse.body().string() : "";
                        log.warn("Poll task failed: code={}, body={}, will retry...", getResponse.code(), errorBody);
                        Thread.sleep(pollIntervalSeconds * 1000L);
                        retries++;
                        continue;
                    }

                    String getResponseBody = getResponse.body().string();
                    log.debug("Poll response: {}", getResponseBody);
                    
                    JsonNode getJsonNode = objectMapper.readTree(getResponseBody);
                    String status = extractStatus(getJsonNode);
                    log.info("Task {} status: {}", taskId, status);

                    if ("succeeded".equalsIgnoreCase(status)) {
                        log.info("Task {} succeeded", taskId);
                        String resultUrl = extractResultUrl(getJsonNode);
                        log.info("Generated video URL: {}", resultUrl);
                        return new GenerationResult(resultUrl, getResponseBody);
                    } else if ("failed".equalsIgnoreCase(status)) {
                        log.error("Task {} failed", taskId);
                        String errorMsg = extractErrorMessage(getJsonNode);
                        throw new RuntimeException("Video generation failed: " + errorMsg);
                    } else if ("running".equalsIgnoreCase(status)) {
                        log.debug("Task {} is still running...", taskId);
                    }
                }

                Thread.sleep(pollIntervalSeconds * 1000L);
                retries++;
            }

            throw new RuntimeException("Task timed out after " + retries + " poll attempts");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Polling interrupted", e);
        } catch (Exception e) {
            log.error("Error polling task status", e);
            throw new RuntimeException("Error polling task status: " + e.getMessage(), e);
        }
    }

    private String extractStatus(JsonNode getResponse) {
        try {
            if (getResponse.has("status")) {
                return getResponse.get("status").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract status from response: {}", e.getMessage());
        }
        return "unknown";
    }



    private String extractResultUrl(JsonNode getResponse) {
        try {
            if (getResponse.has("content")) {
                JsonNode contentNode = getResponse.get("content");
                if (contentNode.has("video_url")) {
                    return contentNode.get("video_url").asText();
                }
            }
            if (getResponse.has("result")) {
                JsonNode resultNode = getResponse.get("result");
                if (resultNode.has("url")) {
                    return resultNode.get("url").asText();
                }
                if (resultNode.has("video_url")) {
                    return resultNode.get("video_url").asText();
                }
                if (resultNode.isArray() && resultNode.size() > 0) {
                    return resultNode.get(0).asText();
                }
            }
            if (getResponse.has("video_url")) {
                return getResponse.get("video_url").asText();
            }
            if (getResponse.has("output_url")) {
                return getResponse.get("output_url").asText();
            }
            log.warn("Could not find video URL in response");
        } catch (Exception e) {
            log.warn("Failed to extract result URL from response: {}", e.getMessage());
        }
        return null;
    }

    private String extractErrorMessage(JsonNode getResponse) {
        try {
            if (getResponse.has("error")) {
                JsonNode errorNode = getResponse.get("error");
                if (errorNode.has("message")) {
                    return errorNode.get("message").asText();
                }
                if (errorNode.isTextual()) {
                    return errorNode.asText();
                }
            }
            if (getResponse.has("message")) {
                return getResponse.get("message").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract error message: {}", e.getMessage());
        }
        return "Unknown error";
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    private boolean isImageFile(String extension) {
        return List.of("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }

    private boolean isVideoFile(String extension) {
        return List.of("mp4", "mov", "avi", "mkv", "webm", "flv").contains(extension);
    }

    private String getResolutionDescription(String resolution) {
        if (resolution == null) return "";
        return switch (resolution.toLowerCase()) {
            case "480p" -> "视频分辨率为480p（标清）";
            case "720p" -> "视频分辨率为720p（高清）";
            case "1080p" -> "视频分辨率为1080p（全高清）";
            default -> "";
        };
    }
}
