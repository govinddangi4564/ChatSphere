package com.chatsphere.websocket.dto;

import java.time.Instant;

public record ReceiptNotification(
        String messagePublicId,
        String conversationPublicId,
        String recipientPublicId,
        String status,
        Instant timestamp
) {}
