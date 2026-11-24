package com.chatsphere.conversation.dto;

import com.chatsphere.common.enums.ConversationType;

import java.time.Instant;
import java.util.List;

public record ConversationDetailsResponse(
        String publicId,
        ConversationType type,
        String title,
        String imageUrl,
        Instant createdAt,
        Instant lastMessageAt,
        List<ConversationParticipantResponse> participants,
        boolean muted,
        boolean archived,
        int unreadCount,
        String role
) {}
