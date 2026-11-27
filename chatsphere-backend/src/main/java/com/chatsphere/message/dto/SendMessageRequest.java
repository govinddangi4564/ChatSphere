package com.chatsphere.message.dto;

import com.chatsphere.common.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SendMessageRequest(
        @NotBlank(message = "Conversation public ID is required")
        String conversationPublicId,

        @NotBlank(message = "Client message ID is required")
        @Size(max = 64, message = "Client message ID cannot exceed 64 characters")
        String clientMessageId,

        @Size(max = 4000, message = "Message content cannot exceed 4000 characters")
        String content,

        MessageType messageType,

        List<String> attachmentPublicIds,

        String replyToMessagePublicId
) {
    public SendMessageRequest(
            String conversationPublicId,
            String clientMessageId,
            String content,
            MessageType messageType
    ) {
        this(conversationPublicId, clientMessageId, content, messageType, List.of(), null);
    }

    public SendMessageRequest(
            String conversationPublicId,
            String clientMessageId,
            String content,
            MessageType messageType,
            List<String> attachmentPublicIds
    ) {
        this(conversationPublicId, clientMessageId, content, messageType, attachmentPublicIds, null);
    }
}
