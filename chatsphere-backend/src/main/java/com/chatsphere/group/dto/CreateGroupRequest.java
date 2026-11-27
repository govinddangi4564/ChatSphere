package com.chatsphere.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
        @NotBlank(message = "Group title is required")
        @Size(max = 100, message = "Group title must not exceed 100 characters")
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @Size(max = 255, message = "Image URL must not exceed 255 characters")
        String imageUrl,

        List<String> memberPublicIds
) {}
