package com.xteam.video.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DistributedConcurrencyManager {

    private static final Logger log = LoggerFactory.getLogger(DistributedConcurrencyManager.class);
    private static final String CONCURRENT_COUNTER_PREFIX = "video:concurrent:";

    private final StringRedisTemplate redisTemplate;
    private final ThirdPartyServiceConfig serviceConfig;

    public DistributedConcurrencyManager(StringRedisTemplate redisTemplate, ThirdPartyServiceConfig serviceConfig) {
        this.redisTemplate = redisTemplate;
        this.serviceConfig = serviceConfig;
    }

    public boolean acquireToken(String serviceKey) {
        String counterKey = CONCURRENT_COUNTER_PREFIX + serviceKey;
        int maxConcurrent = serviceConfig.getMaxConcurrent(serviceKey);

        try {
            Long current = redisTemplate.opsForValue().increment(counterKey);
            
            if (current == null) {
                log.warn("Failed to increment counter for service: {}", serviceKey);
                return false;
            }

            if (current > maxConcurrent) {
                redisTemplate.opsForValue().decrement(counterKey);
                log.debug("Service {} is at max concurrent limit ({}/{}), skipping", 
                          serviceKey, current - 1, maxConcurrent);
                return false;
            }

            log.debug("Acquired token for service {}: current={}/{}", 
                      serviceKey, current, maxConcurrent);
            return true;

        } catch (Exception e) {
            log.error("Failed to acquire token for service {}: {}", serviceKey, e.getMessage());
            return false;
        }
    }

    public void releaseToken(String serviceKey) {
        String counterKey = CONCURRENT_COUNTER_PREFIX + serviceKey;
        
        try {
            Long current = redisTemplate.opsForValue().decrement(counterKey);
            if (current != null && current < 0) {
                redisTemplate.opsForValue().set(counterKey, "0");
            }
            log.debug("Released token for service {}: current={}", serviceKey, current != null ? current : 0);
        } catch (Exception e) {
            log.error("Failed to release token for service {}: {}", serviceKey, e.getMessage());
        }
    }

    public int getCurrentConcurrent(String serviceKey) {
        String counterKey = CONCURRENT_COUNTER_PREFIX + serviceKey;
        String value = redisTemplate.opsForValue().get(counterKey);
        return value != null ? Integer.parseInt(value) : 0;
    }

    public int getMaxConcurrent(String serviceKey) {
        return serviceConfig.getMaxConcurrent(serviceKey);
    }
}