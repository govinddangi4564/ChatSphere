package com.chatsphere.attachment.dto;

import com.chatsphere.common.enums.MessageType;
import java.time.Instant;

public record AttachmentResponse(
        String publicId,
        String fileName,
        MessageType fileType,
        String mimeType,
        Long fileSize,
        String storageUrl,
        String thumbnailUrl,
        Instant createdAt
) {}
