package com.chatsphere.group.dto;

import com.chatsphere.common.enums.MemberRole;

import java.time.Instant;

public record GroupMemberResponse(
        String publicId,
        String username,
        String fullName,
        String profileImageUrl,
        MemberRole role,
        Instant joinedAt
) {}
