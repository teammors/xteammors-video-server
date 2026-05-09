package com.xteam.video.service.impl;

import com.xteam.video.config.ThirdPartyServiceConfig;
import com.xteam.video.dto.VideoGenerationRequest;
import com.xteam.video.service.ThirdPartyServiceClient;
import com.xteam.video.service.thirdparty.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@Service
public class ThirdPartyServiceClientImpl implements ThirdPartyServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyServiceClientImpl.class);
    private static final String SEEDANCE_1_5_PRO_SERVICE_KEY = "seedance-1-5-pro";
    private static final String SEEDANCE_2_0_SERVICE_KEY = "seedance-2-0";
    private static final String VIDU_SERVICE_KEY = "vidu";
    private static final String PIXVERSE_SERVICE_KEY = "pixverse";
    private static final String VEO3_SERVICE_KEY = "veo-3-0";
    private static final String HAPPY_HORSE_SERVICE_KEY = "happy-horse";

    private final RestTemplate restTemplate;
    private final ThirdPartyServiceConfig serviceConfig;
    private final Seedance15VideoGenerator seedance15VideoGenerator;
    private final Seedance2VideoGenerator seedance2VideoGenerator;
    private final ViduVideoGenerator viduVideoGenerator;
    private final PixVerseVideoGenerator pixVerseVideoGenerator;
    private final Veo3VideoGenerator veo3VideoGenerator;
    private final HappyHorseVideoGenerator happyHorseVideoGenerator;

    public ThirdPartyServiceClientImpl(ThirdPartyServiceConfig serviceConfig, 
                                       Seedance15VideoGenerator seedance15VideoGenerator,
                                       Seedance2VideoGenerator seedance2VideoGenerator,
                                       ViduVideoGenerator viduVideoGenerator,
                                       PixVerseVideoGenerator pixVerseVideoGenerator,
                                       Veo3VideoGenerator veo3VideoGenerator,
                                       HappyHorseVideoGenerator happyHorseVideoGenerator) {
        this.restTemplate = new RestTemplate();
        this.serviceConfig = serviceConfig;
        this.seedance15VideoGenerator = seedance15VideoGenerator;
        this.seedance2VideoGenerator = seedance2VideoGenerator;
        this.viduVideoGenerator = viduVideoGenerator;
        this.pixVerseVideoGenerator = pixVerseVideoGenerator;
        this.veo3VideoGenerator = veo3VideoGenerator;
        this.happyHorseVideoGenerator = happyHorseVideoGenerator;
    }

    @Override
    public String generateVideo(String filePath, String textContent, String serviceKey) {
        log.info("Calling third-party service: {} with file: {}", serviceKey, filePath);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = happyHorseVideoGenerator.generateVideo(filePath, textContent);
            return result.getVideoUrl();
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = veo3VideoGenerator.generateVideo(filePath, textContent);
            return result.getVideoUrl();
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = pixVerseVideoGenerator.generateVideo(filePath, textContent);
            return result.getVideoUrl();
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = seedance15VideoGenerator.generateVideo(filePath, textContent);
            return result.getVideoUrl();
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = seedance2VideoGenerator.generateVideo(filePath, textContent, serviceKey);
            return result.getVideoUrl();
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = viduVideoGenerator.generateVideo(filePath, textContent, serviceKey);
            return result.getVideoUrl();
        }

        return callGenericService(filePath, textContent, serviceKey);
    }

    @Override
    public String generateVideo(VideoGenerationRequest request, String serviceKey) {
        log.info("Calling third-party service: {} with VideoGenerationRequest", serviceKey);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = happyHorseVideoGenerator.generateVideo(request);
            return result.getVideoUrl();
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = veo3VideoGenerator.generateVideo(request);
            return result.getVideoUrl();
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = pixVerseVideoGenerator.generateVideo(request);
            return result.getVideoUrl();
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = seedance15VideoGenerator.generateVideo(request);
            return result.getVideoUrl();
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = seedance2VideoGenerator.generateVideo(request, serviceKey);
            return result.getVideoUrl();
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            GenerationResult result = viduVideoGenerator.generateVideo(request, serviceKey);
            return result.getVideoUrl();
        }

        return callGenericService(request, serviceKey);
    }

    @Override
    public GenerationResult generateVideoWithResult(String filePath, String textContent, String serviceKey) {
        log.info("Calling third-party service: {} with file: {}", serviceKey, filePath);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            return happyHorseVideoGenerator.generateVideo(filePath, textContent);
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            return veo3VideoGenerator.generateVideo(filePath, textContent);
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            return pixVerseVideoGenerator.generateVideo(filePath, textContent);
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            return seedance15VideoGenerator.generateVideo(filePath, textContent);
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            return seedance2VideoGenerator.generateVideo(filePath, textContent, serviceKey);
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            return viduVideoGenerator.generateVideo(filePath, textContent, serviceKey);
        }

        String resultUrl = callGenericService(filePath, textContent, serviceKey);
        return new GenerationResult(resultUrl, null);
    }

    @Override
    public GenerationResult generateVideoWithResult(VideoGenerationRequest request, String serviceKey) {
        log.info("Calling third-party service: {} with VideoGenerationRequest", serviceKey);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            return happyHorseVideoGenerator.generateVideo(request);
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            return veo3VideoGenerator.generateVideo(request);
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            return pixVerseVideoGenerator.generateVideo(request);
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            return seedance15VideoGenerator.generateVideo(request);
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            return seedance2VideoGenerator.generateVideo(request, serviceKey);
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            return viduVideoGenerator.generateVideo(request, serviceKey);
        }

        String resultUrl = callGenericService(request, serviceKey);
        return new GenerationResult(resultUrl, null);
    }

    @Override
    public String getModelTaskId(String filePath, String textContent, String serviceKey) {
        log.info("Getting model task ID for service: {} with file: {}", serviceKey, filePath);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            return happyHorseVideoGenerator.getModelTaskId(filePath, textContent);
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            return veo3VideoGenerator.getModelTaskId(filePath, textContent);
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            return pixVerseVideoGenerator.getModelTaskId(filePath, textContent);
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            return seedance15VideoGenerator.getModelTaskId(filePath, textContent);
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            return seedance2VideoGenerator.getModelTaskId(filePath, textContent, serviceKey);
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            return viduVideoGenerator.getModelTaskId(filePath, textContent, serviceKey);
        }

        throw new RuntimeException("不支持的服务: " + serviceKey);
    }

    @Override
    public String getModelTaskId(VideoGenerationRequest request, String serviceKey) {
        log.info("Getting model task ID for service: {} with VideoGenerationRequest", serviceKey);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            return happyHorseVideoGenerator.getModelTaskId(request);
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            return veo3VideoGenerator.getModelTaskId(request);
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            return pixVerseVideoGenerator.getModelTaskId(request);
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            return seedance15VideoGenerator.getModelTaskId(request);
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            return seedance2VideoGenerator.getModelTaskId(request, serviceKey);
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            return viduVideoGenerator.getModelTaskId(request, serviceKey);
        }

        throw new RuntimeException("不支持的服务: " + serviceKey);
    }

    @Override
    public GenerationResult pollForExistingTask(String modelTaskId, String serviceKey) {
        log.info("Polling for existing task: {} with service: {}", modelTaskId, serviceKey);

        if (HAPPY_HORSE_SERVICE_KEY.equals(serviceKey)) {
            return happyHorseVideoGenerator.pollForExistingTask(modelTaskId);
        }
        if (VEO3_SERVICE_KEY.equals(serviceKey)) {
            return veo3VideoGenerator.pollForExistingTask(modelTaskId);
        }
        if (PIXVERSE_SERVICE_KEY.equals(serviceKey)) {
            return pixVerseVideoGenerator.pollForExistingTask(modelTaskId);
        }
        if (SEEDANCE_1_5_PRO_SERVICE_KEY.equals(serviceKey)) {
            return seedance15VideoGenerator.pollForExistingTask(modelTaskId);
        }
        if (SEEDANCE_2_0_SERVICE_KEY.equals(serviceKey)) {
            return seedance2VideoGenerator.pollForExistingTask(modelTaskId, serviceKey);
        }
        if (VIDU_SERVICE_KEY.equals(serviceKey)) {
            return viduVideoGenerator.pollForExistingTask(modelTaskId, serviceKey);
        }

        throw new RuntimeException("不支持的服务: " + serviceKey);
    }

    private String callGenericService(String filePath, String textContent, String serviceKey) {
        try {
            String apiUrl = serviceConfig.getApiUrl(serviceKey);
            String apiKey = serviceConfig.getApiKey(serviceKey);

            if (apiUrl == null || apiKey == null) {
                throw new RuntimeException("未配置的服务: " + serviceKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (filePath != null && !filePath.isEmpty()) {
                body.add("file", new FileSystemResource(new File(filePath)));
            }
            body.add("text", textContent);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("result_url")) {
                    String resultUrl = (String) responseBody.get("result_url");
                    log.info("Video generation successful, result URL: {}", resultUrl);
                    return resultUrl;
                }
            }

            log.error("Third-party service returned error: {}", response.getStatusCode());
            throw new RuntimeException("视频生成失败");

        } catch (Exception e) {
            log.error("Failed to call third-party service: {}", e.getMessage(), e);
            throw new RuntimeException("调用第三方服务失败: " + e.getMessage(), e);
        }
    }

    private String callGenericService(VideoGenerationRequest request, String serviceKey) {
        try {
            String apiUrl = serviceConfig.getApiUrl(serviceKey);
            String apiKey = serviceConfig.getApiKey(serviceKey);

            if (apiUrl == null || apiKey == null) {
                throw new RuntimeException("未配置的服务: " + serviceKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("text", request.getTextContent());
            body.add("generateAudio", String.valueOf(request.getGenerateAudio()));
            body.add("videoRatio", request.getVideoRatio());
            body.add("videoDuration", String.valueOf(request.getVideoDuration()));
            body.add("showWatermark", String.valueOf(request.getShowWatermark()));

            if (request.getResources() != null) {
                for (int i = 0; i < request.getResources().size(); i++) {
                    var resource = request.getResources().get(i);
                    body.add("resource_" + i, resource.getUrl());
                    body.add("resource_type_" + i, resource.getType().name());
                }
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("result_url")) {
                    String resultUrl = (String) responseBody.get("result_url");
                    log.info("Video generation successful, result URL: {}", resultUrl);
                    return resultUrl;
                }
            }

            log.error("Third-party service returned error: {}", response.getStatusCode());
            throw new RuntimeException("视频生成失败");

        } catch (Exception e) {
            log.error("Failed to call third-party service: {}", e.getMessage(), e);
            throw new RuntimeException("调用第三方服务失败: " + e.getMessage(), e);
        }
    }
}