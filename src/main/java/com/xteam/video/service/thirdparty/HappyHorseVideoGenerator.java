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
public class HappyHorseVideoGenerator {

    private static final Logger log = LoggerFactory.getLogger(HappyHorseVideoGenerator.class);
    private static final String SERVICE_KEY = "happy-horse";

    private final ThirdPartyServiceConfig serviceConfig;
    private OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HappyHorseVideoGenerator(ThirdPartyServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("HappyHorseVideoGenerator initialized for model: {}", serviceConfig.getModelId(SERVICE_KEY));
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            log.info("HappyHorseVideoGenerator shutdown");
        }
    }

    public GenerationResult generateVideo(String filePath, String textContent) {
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setTextContent(textContent);

        if (filePath != null && !filePath.isEmpty()) {
            String extension = getFileExtension(filePath).toLowerCase();
            if (isImageFile(extension)) {
                request.addResource(ResourceItem.image(filePath));
            }
        }

        return generateVideo(request);
    }

    public GenerationResult generateVideo(VideoGenerationRequest request) {
        String modelId = serviceConfig.getModelId(SERVICE_KEY);
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        log.info("Starting video generation with HappyHorse: modelId={}", modelId);

        try {
            String taskId = createVideoGenerationTask(request, modelId, apiKey, apiUrl);
            log.info("Video generation task created successfully, taskId: {}", taskId);

            return pollTaskStatus(taskId, apiKey, apiUrl);

        } catch (Exception e) {
            log.error("Video generation failed with HappyHorse", e);
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
            }
        }

        return getModelTaskId(request);
    }

    public String getModelTaskId(VideoGenerationRequest request) {
        String modelId = serviceConfig.getModelId(SERVICE_KEY);
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        log.info("Creating video task with HappyHorse: modelId={}", modelId);

        try {
            String taskId = createVideoGenerationTask(request, modelId, apiKey, apiUrl);
            log.info("Video generation task created successfully, taskId: {}", taskId);

            return taskId;

        } catch (Exception e) {
            log.error("Create video task failed with HappyHorse", e);
            throw new RuntimeException("Create video task failed: " + e.getMessage(), e);
        }
    }

    public GenerationResult pollForExistingTask(String modelTaskId) {
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        return pollTaskStatus(modelTaskId, apiKey, apiUrl);
    }

    private String createVideoGenerationTask(VideoGenerationRequest request, String modelId, String apiKey, String apiUrl) {
        String generateUrl = apiUrl + "/api/v1/services/aigc/video-generation/video-synthesis";

        try {
            Map<String, Object> requestBody = buildCreateRequestBody(request, modelId);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            log.debug("Create task request JSON: {}", requestJson);

            Request httpRequest = new Request.Builder()
                    .url(generateUrl)
                    .addHeader("X-DashScope-Async", "enable")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Create video task failed: code={}, body={}", response.code(), errorBody);
                    throw new RuntimeException("Create video task failed: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                log.debug("Create task response: {}", responseBody);

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                JsonNode outputNode = jsonNode.path("output");
                if (outputNode.has("task_id")) {
                    return outputNode.path("task_id").asText();
                }
                
                if (jsonNode.has("task_id")) {
                    return jsonNode.path("task_id").asText();
                }

                throw new RuntimeException("Failed to extract task_id from response");
            }
        } catch (Exception e) {
            log.error("Failed to create video generation task", e);
            throw new RuntimeException("Failed to create video generation task: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildCreateRequestBody(VideoGenerationRequest request, String modelId) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId != null ? modelId : "happyhorse-1.0-i2v");

        Map<String, Object> input = new HashMap<>();
        String prompt = request.getTextContent();
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Generate a video";
        }
        input.put("prompt", prompt);

        if (request.getResources() != null && !request.getResources().isEmpty()) {
            List<Map<String, Object>> mediaList = new ArrayList<>();
            for (ResourceItem resource : request.getResources()) {
                if (resource.getType() == ResourceType.IMAGE) {
                    Map<String, Object> mediaItem = new HashMap<>();
                    mediaItem.put("type", "first_frame");
                    mediaItem.put("url", resource.getUrl());
                    mediaList.add(mediaItem);
                }
            }
            if (!mediaList.isEmpty()) {
                input.put("media", mediaList);
            }
        }
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        String resolution = request.getResolution();
        if (resolution != null && !resolution.isEmpty()) {
            if (resolution.equalsIgnoreCase("720p")) {
                parameters.put("resolution", "720P");
            } else if (resolution.equalsIgnoreCase("1080p")) {
                parameters.put("resolution", "1080P");
            } else {
                parameters.put("resolution", "720P");
            }
        } else {
            parameters.put("resolution", "720P");
        }

        Long durationLong = request.getVideoDuration();
        int duration = (durationLong == null || durationLong <= 0) ? 5 : durationLong.intValue();
        parameters.put("duration", duration);

        requestBody.put("parameters", parameters);

        log.debug("Final request body: {}", requestBody);
        return requestBody;
    }

    private GenerationResult pollTaskStatus(String taskId, String apiKey, String apiUrl) {
        int pollIntervalSeconds = 5;
        int maxPollRetries = 120;
        int retries = 0;

        try {
            while (retries < maxPollRetries) {
                String statusUrl = apiUrl + "/api/v1/tasks/" + taskId;

                Request request = new Request.Builder()
                        .url(statusUrl)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        log.warn("Poll task failed: code={}, body={}, will retry...", response.code(), errorBody);
                        Thread.sleep(pollIntervalSeconds * 1000L);
                        retries++;
                        continue;
                    }

                    String responseBody = response.body().string();
                    log.debug("Poll response: {}", responseBody);

                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    JsonNode outputNode = jsonNode.path("output");
                    String status = outputNode.path("task_status").asText("");

                    log.info("Task {} status: {}", taskId, status);

                    if ("SUCCEEDED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                        log.info("Task {} completed successfully", taskId);
                        
                        String videoUrl = null;
                        
                        // 尝试从 results 数组中获取
                        JsonNode resultsNode = outputNode.path("results");
                        if (resultsNode.isArray() && resultsNode.size() > 0) {
                            videoUrl = resultsNode.get(0).path("url").asText("");
                            if (videoUrl.isEmpty()) {
                                videoUrl = resultsNode.get(0).asText("");
                            }
                        }
                        
                        // 尝试从 video_url 字段获取（HappyHorse API 的返回格式）
                        if (videoUrl == null || videoUrl.isEmpty()) {
                            if (outputNode.has("video_url")) {
                                videoUrl = outputNode.path("video_url").asText();
                            }
                        }
                        
                        // 尝试从 url 字段获取
                        if (videoUrl == null || videoUrl.isEmpty()) {
                            if (outputNode.has("url")) {
                                videoUrl = outputNode.path("url").asText();
                            }
                        }
                        
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            log.info("Generated video URL: {}", videoUrl);
                            return new GenerationResult(videoUrl, responseBody);
                        } else {
                            log.warn("Could not find video URL in response");
                        }
                    } else if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                        log.error("Task {} failed", taskId);
                        String errorMsg = outputNode.path("error").asText("Unknown error");
                        throw new RuntimeException("Video generation failed: " + errorMsg);
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

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    private boolean isImageFile(String extension) {
        return List.of("png", "webp", "jpeg", "jpg").contains(extension);
    }
}
