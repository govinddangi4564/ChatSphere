package com.chatsphere.notification.service;

import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.NotificationType;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.notification.dto.NotificationResponse;
import com.chatsphere.notification.dto.UnreadNotificationCountResponse;
import com.chatsphere.notification.entity.Notification;
import com.chatsphere.notification.repository.NotificationRepository;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "isRead");
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates an in-app notification with deduplication check and real-time STOMP delivery.
     */
    public NotificationResponse createNotification(
            User recipient,
            NotificationType type,
            String title,
            String message,
            String referenceId
    ) {
        if (recipient == null) {
            return null;
        }

        // Deduplication: prevent duplicate unread notifications for identical user, type, and reference
        if (referenceId != null && notificationRepository.existsByUserIdAndNotificationTypeAndReferenceIdAndIsReadFalse(
                recipient.getId(), type, referenceId)) {
            log.debug("Duplicate unread notification suppressed for user {} type {} ref {}",
                    recipient.getUsername(), type, referenceId);
            return null;
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = mapToResponse(saved);

        // Real-time push over /user/queue/notifications
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getUsername(),
                    "/queue/notifications",
                    response
            );
            log.debug("Real-time notification pushed to user {} on /queue/notifications", recipient.getUsername());
        } catch (Exception e) {
            log.warn("Failed to push real-time notification to user {}: {}", recipient.getUsername(), e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String currentUsername, Boolean isRead, Pageable pageable) {
        User user = getUserByUsername(currentUsername);
        Pageable safePageable = validateAndSanitizePageable(pageable);

        Page<Notification> pageResult;
        if (isRead != null) {
            pageResult = notificationRepository.findByUserIdAndIsRead(user.getId(), isRead, safePageable);
        } else {
            pageResult = notificationRepository.findByUserId(user.getId(), safePageable);
        }

        List<NotificationResponse> dtos = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(dtos, pageResult);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(String currentUsername) {
        User user = getUserByUsername(currentUsername);
        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return new UnreadNotificationCountResponse(count);
    }

    public NotificationResponse markAsRead(String currentUsername, String publicId) {
        User user = getUserByUsername(currentUsername);
        Notification notification = notificationRepository.findByPublicIdAndUserId(publicId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "publicId", publicId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return mapToResponse(updated);
    }

    public void markAllAsRead(String currentUsername) {
        User user = getUserByUsername(currentUsername);
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getPublicId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private Pageable validateAndSanitizePageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }

        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, pageable.getPageSize());

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<Sort.Order> safeOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String prop = order.getProperty();
            if (!ALLOWED_SORTS.contains(prop)) {
                throw new BadRequestException("Invalid sort field: '" + prop + "'. Permitted fields: " + ALLOWED_SORTS);
            }
            safeOrders.add(new Sort.Order(order.getDirection(), prop));
        }

        return PageRequest.of(page, size, Sort.by(safeOrders));
    }
}
