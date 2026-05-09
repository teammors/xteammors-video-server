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
public class ViduVideoGenerator {

    private static final Logger log = LoggerFactory.getLogger(ViduVideoGenerator.class);
    private static final String VIDU_SERVICE_KEY_PREFIX = "vidu-";

    private final ThirdPartyServiceConfig serviceConfig;
    private OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ViduVideoGenerator(ThirdPartyServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("ViduVideoGenerator initialized");
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            log.info("ViduVideoGenerator shutdown");
        }
    }

    public GenerationResult generateVideo(String filePath, String textContent, String serviceKey) {
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setTextContent(textContent);
        
        if (filePath != null && !filePath.isEmpty()) {
            String extension = getFileExtension(filePath).toLowerCase();
            if (isImageFile(extension)) {
                request.addResource(ResourceItem.image(filePath));
            }
        }
        
        return generateVideo(request, serviceKey);
    }

    public GenerationResult generateVideo(VideoGenerationRequest request, String serviceKey) {
        String model = serviceConfig.getModelId(serviceKey);
        String apiKey = serviceConfig.getApiKey(serviceKey);
        String apiUrl = serviceConfig.getApiUrl(serviceKey);

        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://api.vidu.com/ent/v2/img2video";
        }

        log.info("Starting video generation with Vidu: model={}, resolution={}, serviceKey={}",
                model, request.getResolution(), serviceKey);

        try {
            Map<String, Object> createRequestBody = buildCreateRequestBody(request, model);
            String createRequestJson = objectMapper.writeValueAsString(createRequestBody);
            
            log.debug("Create request JSON: {}", createRequestJson);

            Request createHttpRequest = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Token " + apiKey)
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
                
                return pollTaskStatus(taskId, apiKey, apiUrl);
            }

        } catch (Exception e) {
            log.error("Video generation failed with Vidu", e);
            throw new RuntimeException("Video generation failed: " + e.getMessage(), e);
        }
    }

    public String getModelTaskId(String filePath, String textContent, String serviceKey) {
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setTextContent(textContent);
        
        if (filePath != null && !filePath.isEmpty()) {
            String extension = getFileExtension(filePath).toLowerCase();
            if (isImageFile(extension)) {
                request.addResource(ResourceItem.image(filePath));
            }
        }
        
        return getModelTaskId(request, serviceKey);
    }

    public String getModelTaskId(VideoGenerationRequest request, String serviceKey) {
        String model = serviceConfig.getModelId(serviceKey);
        String apiKey = serviceConfig.getApiKey(serviceKey);
        String apiUrl = serviceConfig.getApiUrl(serviceKey);

        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://api.vidu.com/ent/v2/img2video";
        }

        log.info("Creating video task with Vidu: model={}, resolution={}, serviceKey={}",
                model, request.getResolution(), serviceKey);

        try {
            Map<String, Object> createRequestBody = buildCreateRequestBody(request, model);
            String createRequestJson = objectMapper.writeValueAsString(createRequestBody);
            
            log.debug("Create request JSON: {}", createRequestJson);

            Request createHttpRequest = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Token " + apiKey)
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
            log.error("Create video task failed with Vidu", e);
            throw new RuntimeException("Create video task failed: " + e.getMessage(), e);
        }
    }

    public GenerationResult pollForExistingTask(String modelTaskId, String serviceKey) {
        String apiKey = serviceConfig.getApiKey(serviceKey);
        String apiUrl = serviceConfig.getApiUrl(serviceKey);
        
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://api.vidu.com/ent/v2/img2video";
        }
        
        return pollTaskStatus(modelTaskId, apiKey, apiUrl);
    }

    private Map<String, Object> buildCreateRequestBody(VideoGenerationRequest request, String model) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<String> imageUrls = new ArrayList<>();
        if (request.getResources() != null) {
            for (ResourceItem resource : request.getResources()) {
                if (resource.getType() == ResourceType.IMAGE) {
                    imageUrls.add(resource.getUrl());
                    log.debug("Adding reference image: {}", resource.getUrl());
                }
            }
        }
        
        if (!imageUrls.isEmpty()) {
            requestBody.put("images", imageUrls);
        }

        if (request.getTextContent() != null && !request.getTextContent().isEmpty()) {
            requestBody.put("prompt", request.getTextContent());
        }

        requestBody.put("audio", request.getGenerateAudio() != null ? request.getGenerateAudio() : true);
        
        if (request.getResolution() != null && !request.getResolution().isEmpty()) {
            requestBody.put("resolution", request.getResolution());
        }

        requestBody.put("movement_amplitude", "auto");
        requestBody.put("off_peak", false);

        log.debug("Final request body: {}", requestBody);
        return requestBody;
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

    private GenerationResult pollTaskStatus(String taskId, String apiKey, String apiUrl) {
        int pollIntervalSeconds = 10;
        int maxPollRetries = 60;
        int retries = 0;
        
        try {
            // 使用正确的 Vidu 查询 URL 格式
            String queryUrl = "https://api.vidu.com/ent/v2/tasks/" + taskId + "/creations";
            log.info("Querying task status at: {}", queryUrl);
            
            while (retries < maxPollRetries) {
                
                Request getHttpRequest = new Request.Builder()
                        .url(queryUrl)
                        .addHeader("Authorization", "Token " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();

                try (Response getResponse = client.newCall(getHttpRequest).execute()) {
                    if (!getResponse.isSuccessful()) {
                        String errorBody = getResponse.body() != null ? getResponse.body().string() : "";
                        log.warn("Poll task failed: code={}, body={}, will retry", getResponse.code(), errorBody);
                        
                        Thread.sleep(pollIntervalSeconds * 1000L);
                        retries++;
                        continue;
                    }

                    String getResponseBody = getResponse.body().string();
                    log.debug("Poll response: {}", getResponseBody);
                    
                    JsonNode getJsonNode = objectMapper.readTree(getResponseBody);
                    String status = extractStatus(getJsonNode);
                    log.info("Task {} status: {}", taskId, status);

                    if ("succeeded".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                        log.info("Task {} succeeded", taskId);
                        String resultUrl = extractResultUrl(getJsonNode);
                        log.info("Generated video URL: {}", resultUrl);
                        return new GenerationResult(resultUrl, getResponseBody);
                    } else if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                        log.error("Task {} failed", taskId);
                        String errorMsg = extractErrorMessage(getJsonNode);
                        throw new RuntimeException("Video generation failed: " + errorMsg);
                    } else if ("processing".equalsIgnoreCase(status) || "running".equalsIgnoreCase(status)) {
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
            if (getResponse.has("state")) {
                return getResponse.get("state").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract status from response: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 从 Vidu 响应中提取视频 URL
     * @param getResponse Vidu API 响应
     * @return 视频 URL
     */
    private String extractResultUrl(JsonNode getResponse) {
        try {
            // 从 Vidu 的响应结构中提取视频 URL
            if (getResponse.has("creations") && getResponse.get("creations").isArray()) {
                JsonNode creations = getResponse.get("creations");
                if (creations.size() > 0) {
                    JsonNode firstCreation = creations.get(0);
                    if (firstCreation.has("url")) {
                        return firstCreation.get("url").asText();
                    }
                }
            }
            // 兼容其他可能的结构
            if (getResponse.has("video_url")) {
                return getResponse.get("video_url").asText();
            }
            if (getResponse.has("result_url")) {
                return getResponse.get("result_url").asText();
            }
            if (getResponse.has("url")) {
                return getResponse.get("url").asText();
            }
            log.warn("Could not find video URL in response, returning null");
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
}
