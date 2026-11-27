package com.chatsphere.group.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddGroupMembersRequest(
        @NotEmpty(message = "At least one member publicId must be provided")
        List<String> memberPublicIds
) {}
