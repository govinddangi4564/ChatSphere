package com.chatsphere.message.dto;

import com.chatsphere.attachment.dto.AttachmentResponse;
import com.chatsphere.common.enums.MessageType;

import java.time.Instant;
import java.util.List;

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
        boolean deletedForEveryone,
        List<AttachmentResponse> attachments,
        ReplySnippet replyToMessage,
        boolean isForwarded,
        boolean isPinned,
        Instant pinnedAt,
        String pinnedByUsername,
        List<ReactionResponse> reactions
) {
    public MessageResponse(
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
    ) {
        this(
                publicId,
                clientMessageId,
                conversationPublicId,
                senderPublicId,
                senderUsername,
                senderFullName,
                messageType,
                content,
                createdAt,
                updatedAt,
                edited,
                deletedForEveryone,
                List.of(),
                null,
                false,
                false,
                null,
                null,
                List.of()
        );
    }

    public MessageResponse(
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
            boolean deletedForEveryone,
            List<AttachmentResponse> attachments
    ) {
        this(
                publicId,
                clientMessageId,
                conversationPublicId,
                senderPublicId,
                senderUsername,
                senderFullName,
                messageType,
                content,
                createdAt,
                updatedAt,
                edited,
                deletedForEveryone,
                attachments,
                null,
                false,
                false,
                null,
                null,
                List.of()
        );
    }
}
