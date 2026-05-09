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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PixVerseVideoGenerator {

    private static final Logger log = LoggerFactory.getLogger(PixVerseVideoGenerator.class);
    private static final String SERVICE_KEY = "pixverse";

    private final ThirdPartyServiceConfig serviceConfig;
    private OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PixVerseVideoGenerator(ThirdPartyServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();

        log.info("PixVerseVideoGenerator initialized for model: {}", serviceConfig.getModelId(SERVICE_KEY));
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            log.info("PixVerseVideoGenerator shutdown");
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

        log.info("Starting video generation with PixVerse: modelId={}, resolution={}", modelId, request.getResolution());

        try {
            Long imgId = null;

            if (request.getResources() != null && !request.getResources().isEmpty()) {
                for (ResourceItem resource : request.getResources()) {
                    if (resource.getType() == ResourceType.IMAGE) {
                        imgId = uploadImage(resource.getUrl(), apiKey, apiUrl);
                        log.info("Image uploaded successfully, img_id: {}", imgId);
                        break;
                    }
                }
            }

            Long videoId = createVideoGenerationTask(imgId, request, modelId, apiKey, apiUrl);
            log.info("Video generation task created successfully, video_id: {}", videoId);

            return pollTaskStatus(videoId, apiKey, apiUrl);

        } catch (Exception e) {
            log.error("Video generation failed with PixVerse", e);
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

        log.info("Creating video task with PixVerse: modelId={}, resolution={}", modelId, request.getResolution());

        try {
            Long imgId = null;

            if (request.getResources() != null && !request.getResources().isEmpty()) {
                for (ResourceItem resource : request.getResources()) {
                    if (resource.getType() == ResourceType.IMAGE) {
                        imgId = uploadImage(resource.getUrl(), apiKey, apiUrl);
                        log.info("Image uploaded successfully, img_id: {}", imgId);
                        break;
                    }
                }
            }

            Long videoId = createVideoGenerationTask(imgId, request, modelId, apiKey, apiUrl);
            log.info("Video generation task created successfully, video_id: {}", videoId);

            return String.valueOf(videoId);

        } catch (Exception e) {
            log.error("Create video task failed with PixVerse", e);
            throw new RuntimeException("Create video task failed: " + e.getMessage(), e);
        }
    }

    public GenerationResult pollForExistingTask(String modelTaskId) {
        String apiKey = serviceConfig.getApiKey(SERVICE_KEY);
        String apiUrl = serviceConfig.getApiUrl(SERVICE_KEY);

        try {
            Long videoId = Long.parseLong(modelTaskId);
            return pollTaskStatus(videoId, apiKey, apiUrl);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid video_id format: " + modelTaskId, e);
        }
    }

    private Long uploadImage(String imagePath, String apiKey, String apiUrl) {
        String traceId = UUID.randomUUID().toString();
        String uploadUrl = apiUrl + "/openapi/v2/image/upload";

        try {
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                multipartBuilder.addFormDataPart("image_url", imagePath);
                log.info("Uploading image from URL: {}", imagePath);
            } else {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    throw new RuntimeException("Image file not found: " + imagePath);
                }
                RequestBody fileBody = RequestBody.create(
                        imageFile,
                        MediaType.parse("image/*")
                );
                multipartBuilder.addFormDataPart("image", imageFile.getName(), fileBody);
                log.info("Uploading image from file: {}", imagePath);
            }

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("API-KEY", apiKey)
                    .addHeader("Ai-trace-id", traceId)
                    .post(multipartBuilder.build())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Image upload failed: code={}, body={}", response.code(), errorBody);
                    throw new RuntimeException("Image upload failed: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                log.debug("Image upload response: {}", responseBody);

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                int errCode = jsonNode.path("ErrCode").asInt();
                if (errCode != 0) {
                    String errMsg = jsonNode.path("ErrMsg").asText();
                    throw new RuntimeException("Image upload failed: " + errCode + " - " + errMsg);
                }

                return jsonNode.path("Resp").path("img_id").asLong();
            }
        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    private Long createVideoGenerationTask(Long imgId, VideoGenerationRequest request, String modelId, String apiKey, String apiUrl) {
        String traceId = UUID.randomUUID().toString();
        String generateUrl = apiUrl + "/openapi/v2/video/img/generate";

        try {
            Map<String, Object> requestBody = buildCreateRequestBody(imgId, request, modelId);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            log.debug("Create task request JSON: {}", requestJson);

            Request httpRequest = new Request.Builder()
                    .url(generateUrl)
                    .addHeader("API-KEY", apiKey)
                    .addHeader("Ai-trace-id", traceId)
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
                int errCode = jsonNode.path("ErrCode").asInt();
                if (errCode != 0) {
                    String errMsg = jsonNode.path("ErrMsg").asText();
                    throw new RuntimeException("Create video task failed: " + errCode + " - " + errMsg);
                }

                return jsonNode.path("Resp").path("video_id").asLong();
            }
        } catch (Exception e) {
            log.error("Failed to create video generation task", e);
            throw new RuntimeException("Failed to create video generation task: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildCreateRequestBody(Long imgId, VideoGenerationRequest request, String modelId) {
        Map<String, Object> requestBody = new java.util.HashMap<>();

        Long durationLong = request.getVideoDuration();
        int duration = (durationLong == null || durationLong <= 0) ? 5 : durationLong.intValue();
        requestBody.put("duration", duration);

        if (imgId != null) {
            requestBody.put("img_id", imgId);
        }

        requestBody.put("model", modelId != null ? modelId : "v6");

        String prompt = request.getTextContent();
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Generate a video";
        }
        requestBody.put("prompt", prompt);

        String quality = getQualityFromResolution(request.getResolution());
        requestBody.put("quality", quality);

        requestBody.put("seed", 0);

        requestBody.put("template_id", 0);
        requestBody.put("motion_mode", "normal");

        log.debug("Final request body: {}", requestBody);
        return requestBody;
    }

    private String getQualityFromResolution(String resolution) {
        if (resolution == null) {
            return "720p";
        }
        return switch (resolution.toLowerCase()) {
            case "360p" -> "360p";
            case "540p" -> "540p";
            case "720p" -> "720p";
            case "1080p" -> "1080p";
            default -> "720p";
        };
    }

    private GenerationResult pollTaskStatus(Long videoId, String apiKey, String apiUrl) {
        int pollIntervalSeconds = 3;
        int maxPollRetries = 120;
        int retries = 0;

        try {
            while (retries < maxPollRetries) {
                String traceId = UUID.randomUUID().toString();
                String statusUrl = apiUrl + "/openapi/v2/video/result/" + videoId;

                Request request = new Request.Builder()
                        .url(statusUrl)
                        .addHeader("API-KEY", apiKey)
                        .addHeader("Ai-trace-id", traceId)
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
                    int errCode = jsonNode.path("ErrCode").asInt();
                    if (errCode != 0) {
                        String errMsg = jsonNode.path("ErrMsg").asText();
                        log.warn("Poll task returned error: {} - {}, will retry...", errCode, errMsg);
                        Thread.sleep(pollIntervalSeconds * 1000L);
                        retries++;
                        continue;
                    }

                    JsonNode respNode = jsonNode.path("Resp");
                    int status = respNode.path("status").asInt();
                    log.info("Video {} status: {}", videoId, status);

                    if (status == 1) {
                        log.info("Video {} generated successfully", videoId);
                        String videoUrl = respNode.path("url").asText();
                        log.info("Generated video URL: {}", videoUrl);
                        return new GenerationResult(videoUrl, responseBody);
                    } else if (status == 7) {
                        log.error("Video {} content review failed", videoId);
                        throw new RuntimeException("Video generation failed: Content review not passed");
                    } else if (status == 8) {
                        log.error("Video {} generation failed", videoId);
                        throw new RuntimeException("Video generation failed");
                    } else if (status == 5) {
                        log.debug("Video {} is still processing...", videoId);
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
