package com.chatsphere.user.dto;

import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;

public record PrivacySettingsResponse(
        PrivacyVisibility profilePhotoVisibility,
        PrivacyVisibility lastSeenVisibility,
        PrivacyVisibility onlineStatusVisibility,
        MessagePermission messagePermission
) {}
