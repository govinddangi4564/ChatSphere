package com.chatsphere.group.dto;

public record GroupCreatorResponse(
        String publicId,
        String username,
        String fullName,
        String profileImageUrl
) {}
