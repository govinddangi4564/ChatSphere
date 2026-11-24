package com.chatsphere.conversation.dto;

import com.chatsphere.common.enums.ConversationType;

import java.time.Instant;

public record ConversationSummaryResponse(
        String publicId,
        ConversationType type,
        String title,
        String imageUrl,
        Instant lastMessageAt,
        String lastMessagePreview,
        int unreadCount,
        boolean muted,
        boolean archived,
        ConversationParticipantResponse participant
) {}
