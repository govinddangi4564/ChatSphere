package com.chatsphere.group.dto;

import com.chatsphere.common.enums.MemberRole;

import java.time.Instant;
import java.util.List;

public record GroupDetailsResponse(
        String publicId,
        String title,
        String description,
        String imageUrl,
        Instant createdAt,
        GroupCreatorResponse createdBy,
        MemberRole currentUserRole,
        List<GroupMemberResponse> members
) {}
