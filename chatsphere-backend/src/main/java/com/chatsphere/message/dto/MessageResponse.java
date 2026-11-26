package com.chatsphere.message.dto;

import com.chatsphere.common.enums.MessageType;

import java.time.Instant;

public record MessageResponse(
        String publicId,
        String clientMessageId,
        String conversationPublicId,
        String senderPublicId,
        String senderUsername,
        String senderFullName,
        MessageType messageType,
        String content,
        Instant createdAt,
        Instant updatedAt,
        boolean edited,
        boolean deletedForEveryone
) {}
