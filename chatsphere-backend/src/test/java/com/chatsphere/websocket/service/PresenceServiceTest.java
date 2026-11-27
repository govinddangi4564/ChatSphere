package com.chatsphere.websocket.service;

import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(userRepository, stringRedisTemplate, 60, 5);
    }

    @Test
    void userConnected_setsKeyInRedisWithTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        presenceService.userConnected("alice");

        verify(valueOperations).set(eq("presence:user:alice"), eq("ONLINE"), eq(Duration.ofSeconds(60)));
        assertTrue(presenceService.isUserOnline("alice"));
    }

    @Test
    void userConnected_redisDown_usesLocalStateGracefully() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis down"));

        assertDoesNotThrow(() -> presenceService.userConnected("bob"));
        assertTrue(presenceService.isUserOnline("bob"));
    }

    @Test
    void userDisconnected_deletesFromRedisAndUpdatesLastSeen() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        User user = new User();
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        presenceService.userConnected("alice");
        presenceService.userDisconnected("alice");

        verify(stringRedisTemplate).delete("presence:user:alice");
        verify(userRepository).save(user);
        assertNotNull(user.getLastSeenAt());
    }
}
