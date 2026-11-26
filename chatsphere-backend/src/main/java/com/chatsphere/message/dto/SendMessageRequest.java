package com.chatsphere.message.dto;

import com.chatsphere.common.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Conversation public ID is required")
        String conversationPublicId,

        @NotBlank(message = "Client message ID is required")
        @Size(max = 64, message = "Client message ID cannot exceed 64 characters")
        String clientMessageId,

        @NotBlank(message = "Message content cannot be blank")
        @Size(max = 4000, message = "Message content cannot exceed 4000 characters")
        String content,

        MessageType messageType
) {}
