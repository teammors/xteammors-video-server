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
public class Veo3VideoGenerator {

    private static final Logger log = LoggerFactory.getLogger(Veo3VideoGenerator.class);
    private static final String SERVICE_KEY = "veo-3-0";

    private final ThirdPartyServiceConfig serviceConfig;
    private OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Veo3VideoGenerator(ThirdPartyServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("Veo3VideoGenerator initialized for model: {}", serviceConfig.getModelId(SERVICE_KEY));
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            log.info("Veo3VideoGenerator shutdown");
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

        log.info("Starting video generation with Veo3: modelId={}", modelId);

        try {
            String taskId = createVideoGenerationTask(request, modelId, apiKey, apiUrl);
            log.info("Video generation task created successfully, taskId: {}", taskId);

            return pollTaskStatus(taskId, apiKey, apiUrl);

        } catch (Exception e) {
            log.error("Video generation failed with Veo3", e);
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

        log.info("Creating video task with Veo3: modelId={}", modelId);

        try {
            String taskId = createVideoGenerationTask(request, modelId, apiKey, apiUrl);
            log.info("Video generation task created successfully, taskId: {}", taskId);

            return taskId;

        } catch (Exception e) {
            log.error("Create video task failed with Veo3", e);
            throw new RuntimeException("Create video task failed: " + e.getMessage(), e);
        }
    }

    public GenerationResult pollForExistingTask(String modelTaskId) {
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        return pollTaskStatus(modelTaskId, apiKey, apiUrl);
    }

    private String createVideoGenerationTask(VideoGenerationRequest request, String modelId, String apiKey, String apiUrl) {
        String generateUrl = apiUrl + "/generate";

        try {
            Map<String, Object> requestBody = buildCreateRequestBody(request, modelId);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            log.debug("Create task request JSON: {}", requestJson);

            Request httpRequest = new Request.Builder()
                    .url(generateUrl)
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
                int code = jsonNode.path("code").asInt();
                if (code != 200) {
                    String message = jsonNode.path("message").asText();
                    throw new RuntimeException("Create video task failed: " + code + " - " + message);
                }

                return jsonNode.path("data").path("task_id").asText();
            }
        } catch (Exception e) {
            log.error("Failed to create video generation task", e);
            throw new RuntimeException("Failed to create video generation task: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildCreateRequestBody(VideoGenerationRequest request, String modelId) {
        Map<String, Object> requestBody = new HashMap<>();

        String prompt = request.getTextContent();
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Generate a video";
        }
        requestBody.put("prompt", prompt);

        requestBody.put("model", modelId != null ? modelId : "veo3-fast");

        if (request.getResources() != null && !request.getResources().isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (ResourceItem resource : request.getResources()) {
                if (resource.getType() == ResourceType.IMAGE) {
                    imageUrls.add(resource.getUrl());
                }
            }
            if (!imageUrls.isEmpty()) {
                requestBody.put("image_urls", imageUrls);
                requestBody.put("aspect_ratio", "Auto");
            }
        } else {
            requestBody.put("aspect_ratio", "16:9");
        }

        if (request.getShowWatermark() != null && request.getShowWatermark()) {
            requestBody.put("watermark", "veo");
        }

        log.debug("Final request body: {}", requestBody);
        return requestBody;
    }

    private GenerationResult pollTaskStatus(String taskId, String apiKey, String apiUrl) {
        int pollIntervalSeconds = 5;
        int maxPollRetries = 120;
        int retries = 0;

        try {
            while (retries < maxPollRetries) {
                String statusUrl = apiUrl + "/feed?task_id=" + taskId;

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
                    int code = jsonNode.path("code").asInt();
                    if (code != 200) {
                        String message = jsonNode.path("message").asText();
                        log.warn("Poll task returned error: {} - {}, will retry...", code, message);
                        Thread.sleep(pollIntervalSeconds * 1000L);
                        retries++;
                        continue;
                    }

                    JsonNode dataNode = jsonNode.path("data");
                    String status = dataNode.path("status").asText();
                    log.info("Task {} status: {}", taskId, status);

                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        log.info("Task {} completed successfully", taskId);
                        JsonNode responseArray = dataNode.path("response");
                        if (responseArray.isArray() && responseArray.size() > 0) {
                            String videoUrl = responseArray.get(0).asText();
                            log.info("Generated video URL: {}", videoUrl);
                            return new GenerationResult(videoUrl, responseBody);
                        }
                    } else if ("FAILED".equalsIgnoreCase(status)) {
                        log.error("Task {} failed", taskId);
                        throw new RuntimeException("Video generation failed");
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
