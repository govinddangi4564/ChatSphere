package com.chatsphere.group.dto;

import com.chatsphere.common.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Member role is required")
        MemberRole role
) {}
