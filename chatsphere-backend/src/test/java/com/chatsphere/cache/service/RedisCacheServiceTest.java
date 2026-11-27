package com.chatsphere.cache.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new RedisCacheService(stringRedisTemplate);
    }

    @Test
    void getOrCompute_cacheMiss_computesFromDbAndPutsInCache() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:profile:1")).thenReturn(null);

        AtomicInteger dbCallCount = new AtomicInteger(0);
        String result = cacheService.getOrCompute(
                "user:profile:1",
                Duration.ofMinutes(5),
                String.class,
                () -> {
                    dbCallCount.incrementAndGet();
                    return "John Doe";
                }
        );

        assertEquals("John Doe", result);
        assertEquals(1, dbCallCount.get());
        verify(valueOperations).set(eq("user:profile:1"), contains("John Doe"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void getOrCompute_cacheHit_returnsCachedValueWithoutCallingDb() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:profile:1")).thenReturn("\"Cached John\"");

        AtomicInteger dbCallCount = new AtomicInteger(0);
        String result = cacheService.getOrCompute(
                "user:profile:1",
                Duration.ofMinutes(5),
                String.class,
                () -> {
                    dbCallCount.incrementAndGet();
                    return "Database John";
                }
        );

        assertEquals("Cached John", result);
        assertEquals(0, dbCallCount.get(), "Database supplier should not be invoked on cache hit");
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }

    @Test
    void evict_deletesKeyFromRedis() {
        cacheService.evict("user:profile:1");
        verify(stringRedisTemplate).delete("user:profile:1");
    }

    @Test
    void getOrCompute_redisUnavailable_fallsBackToDbGracefully() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis connection refused"));

        AtomicInteger dbCallCount = new AtomicInteger(0);
        // Does not throw an exception, returns result directly from DB supplier
        String result = cacheService.getOrCompute(
                "user:profile:fallback",
                Duration.ofMinutes(5),
                String.class,
                () -> {
                    dbCallCount.incrementAndGet();
                    return "Fallback Data";
                }
        );

        assertEquals("Fallback Data", result);
        assertEquals(1, dbCallCount.get());
    }
}
