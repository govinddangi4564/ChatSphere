package com.chatsphere.notification.dto;

import com.chatsphere.common.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String publicId,
        NotificationType notificationType,
        String title,
        String message,
        String referenceId,
        boolean isRead,
        Instant createdAt
) {
}
