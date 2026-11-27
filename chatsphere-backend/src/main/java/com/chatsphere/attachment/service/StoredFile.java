package com.chatsphere.attachment.service;

public record StoredFile(
        String storagePublicId,
        String storageUrl,
        String thumbnailUrl,
        String originalFileName,
        String mimeType,
        long fileSize
) {}
