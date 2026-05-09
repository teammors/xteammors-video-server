package com.xteam.video.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "third-party")
public class ThirdPartyServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyServiceConfig.class);

    private String defaultService;
    private Map<String, ServiceConfig> services = new HashMap<>();

    @PostConstruct
    public void validate() {
        if (defaultService == null || defaultService.isEmpty()) {
            log.warn("third-party.default-service is not configured, please set it in application.yml");
        } else if (!services.containsKey(defaultService)) {
            log.warn("default-service '{}' is not found in services configuration", defaultService);
        }
    }

    public String getDefaultService() {
        return defaultService;
    }

    public void setDefaultService(String defaultService) {
        this.defaultService = defaultService;
    }

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }

    public ServiceConfig getServiceConfig(String serviceKey) {
        return services.get(serviceKey);
    }

    public int getMaxConcurrent(String serviceKey) {
        ServiceConfig config = services.get(serviceKey);
        return config != null ? config.getMaxConcurrent() : 1;
    }

    public String getApiUrl(String serviceKey) {
        ServiceConfig config = services.get(serviceKey);
        return config != null ? config.getApiUrl() : null;
    }

    public String getApiKey(String serviceKey) {
        ServiceConfig config = services.get(serviceKey);
        return config != null ? config.getApiKey() : null;
    }

    public String getModelId(String serviceKey) {
        ServiceConfig config = services.get(serviceKey);
        return config != null ? config.getModelId() : null;
    }

    public List<ServiceOption> getServiceOptions() {
        return services.entrySet().stream()
                .map(entry -> {
                    ServiceConfig config = entry.getValue();
                    return new ServiceOption(entry.getKey(), config.getName(), config.getDescription());
                })
                .collect(Collectors.toList());
    }

    public static class ServiceConfig {
        private String name;
        private String description;
        private String apiUrl;
        private String apiKey;
        private int maxConcurrent = 1;
        private String modelId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }
    }

    public static class ServiceOption {
        private String code;
        private String name;
        private String description;

        public ServiceOption() {}

        public ServiceOption(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}