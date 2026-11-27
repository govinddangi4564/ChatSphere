package com.chatsphere.cache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Cache-Aside fetch with graceful fallback to DB supplier when Redis is down or key is absent.
     */
    public <T> T getOrCompute(String key, Duration ttl, Class<T> clazz, Supplier<T> dbSupplier) {
        T cached = get(key, clazz);
        if (cached != null) {
            log.debug("Cache HIT for key: {}", key);
            return cached;
        }

        log.debug("Cache MISS for key: {}, loading from database", key);
        T computed = dbSupplier.get();
        if (computed != null) {
            put(key, computed, ttl);
        }
        return computed;
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, clazz);
            }
        } catch (Exception e) {
            log.warn("Redis read failure for key '{}'; falling back safely. Error: {}", key, e.getMessage());
        }
        return null;
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Cache PUT for key: {} with TTL {}s", key, ttl.toSeconds());
        } catch (Exception e) {
            log.warn("Redis write failure for key '{}'; continuing without cache. Error: {}", key, e.getMessage());
        }
    }

    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
            log.debug("Cache EVICT for key: {}", key);
        } catch (Exception e) {
            log.warn("Redis evict failure for key '{}': {}", key, e.getMessage());
        }
    }

    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.debug("Cache EVICT pattern: {} ({} keys removed)", pattern, keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis evict pattern failure for pattern '{}': {}", pattern, e.getMessage());
        }
    }
}
