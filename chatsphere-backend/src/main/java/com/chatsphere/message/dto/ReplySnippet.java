package com.chatsphere.message.dto;

public record ReplySnippet(
        String publicId,
        String senderPublicId,
        String senderUsername,
        String senderFullName,
        String content,
        String messageType
) {}
