package com.chatsphere.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Size(max = 255, message = "Bio must not exceed 255 characters")
        String bio,

        @Size(max = 255, message = "Profile image URL must not exceed 255 characters")
        String profileImageUrl
) {}
