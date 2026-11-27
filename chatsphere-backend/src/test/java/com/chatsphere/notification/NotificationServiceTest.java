package com.chatsphere.notification;

import com.chatsphere.common.enums.NotificationType;
import com.chatsphere.notification.dto.NotificationResponse;
import com.chatsphere.notification.dto.UnreadNotificationCountResponse;
import com.chatsphere.notification.entity.Notification;
import com.chatsphere.notification.repository.NotificationRepository;
import com.chatsphere.notification.service.NotificationService;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                messagingTemplate
        );

        user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setFullName("Alice Smith");
        user.setPublicId(UUID.randomUUID().toString());
    }

    @Test
    void createNotification_Success_SavesAndPushesOverWebSocket() {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.NEW_MESSAGE);
        notification.setTitle("New message from Bob");
        notification.setMessage("Bob: Hello Alice");
        notification.setReferenceId("conv-123");
        notification.setRead(false);

        given(notificationRepository.existsByUserIdAndNotificationTypeAndReferenceIdAndIsReadFalse(10L, NotificationType.NEW_MESSAGE, "conv-123"))
                .willReturn(false);
        given(notificationRepository.save(any(Notification.class))).willReturn(notification);

        NotificationResponse response = notificationService.createNotification(
                user,
                NotificationType.NEW_MESSAGE,
                "New message from Bob",
                "Bob: Hello Alice",
                "conv-123"
        );

        assertThat(response).isNotNull();
        assertThat(response.notificationType()).isEqualTo(NotificationType.NEW_MESSAGE);
        assertThat(response.title()).isEqualTo("New message from Bob");
        assertThat(response.isRead()).isFalse();

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("alice"), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    void createNotification_DuplicateSuppressed_ReturnsNull() {
        given(notificationRepository.existsByUserIdAndNotificationTypeAndReferenceIdAndIsReadFalse(10L, NotificationType.NEW_MESSAGE, "conv-123"))
                .willReturn(true);

        NotificationResponse response = notificationService.createNotification(
                user,
                NotificationType.NEW_MESSAGE,
                "New message from Bob",
                "Bob: Hello Alice",
                "conv-123"
        );

        assertThat(response).isNull();
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void getUnreadCount_ReturnsAccurateCount() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user));
        given(notificationRepository.countByUserIdAndIsReadFalse(10L)).willReturn(3L);

        UnreadNotificationCountResponse res = notificationService.getUnreadCount("alice");

        assertThat(res.unreadCount()).isEqualTo(3L);
    }

    @Test
    void markAsRead_Success_SetsIsReadTrue() {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setPublicId("notif-1");
        notification.setRead(false);

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user));
        given(notificationRepository.findByPublicIdAndUserId("notif-1", 10L)).willReturn(Optional.of(notification));
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        NotificationResponse res = notificationService.markAsRead("alice", "notif-1");

        assertThat(res.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllAsRead_Success_CallsRepositoryModifyingQuery() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user));
        given(notificationRepository.markAllAsReadByUserId(10L)).willReturn(5);

        notificationService.markAllAsRead("alice");

        verify(notificationRepository).markAllAsReadByUserId(10L);
    }
}
