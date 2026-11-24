package com.chatsphere.user.dto;

import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;
import jakarta.validation.constraints.NotNull;

public record PrivacySettingsRequest(
        @NotNull(message = "Profile photo visibility is required")
        PrivacyVisibility profilePhotoVisibility,

        @NotNull(message = "Last seen visibility is required")
        PrivacyVisibility lastSeenVisibility,

        @NotNull(message = "Online status visibility is required")
        PrivacyVisibility onlineStatusVisibility,

        @NotNull(message = "Message permission is required")
        MessagePermission messagePermission
) {}
