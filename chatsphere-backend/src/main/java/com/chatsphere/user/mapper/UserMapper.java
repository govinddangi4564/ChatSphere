package com.chatsphere.user.mapper;

import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.contact.dto.ContactResponse;
import com.chatsphere.contact.entity.UserContact;
import com.chatsphere.user.dto.BlockedUserResponse;
import com.chatsphere.user.dto.PrivacySettingsResponse;
import com.chatsphere.user.dto.UserPublicProfileResponse;
import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.BlockedUser;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.entity.UserPrivacySetting;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getPublicId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getBio(),
                user.getAccountStatus() != null ? user.getAccountStatus().name() : null,
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    public static UserPublicProfileResponse toUserPublicProfileResponse(
            User user,
            boolean canViewProfilePhoto,
            boolean canViewLastSeen,
            boolean isContact,
            boolean isBlocked
    ) {
        if (user == null) {
            return null;
        }
        return new UserPublicProfileResponse(
                user.getPublicId(),
                user.getFullName(),
                user.getUsername(),
                canViewProfilePhoto ? user.getProfileImageUrl() : null,
                user.getBio(),
                canViewLastSeen ? user.getLastSeenAt() : null,
                isContact,
                isBlocked
        );
    }

    public static ContactResponse toContactResponse(
            UserContact contact,
            boolean canViewProfilePhoto,
            boolean canViewLastSeen
    ) {
        if (contact == null || contact.getContactUser() == null) {
            return null;
        }
        User target = contact.getContactUser();
        return new ContactResponse(
                target.getPublicId(),
                target.getFullName(),
                target.getUsername(),
                canViewProfilePhoto ? target.getProfileImageUrl() : null,
                target.getBio(),
                canViewLastSeen ? target.getLastSeenAt() : null,
                contact.getCreatedAt()
        );
    }

    public static BlockedUserResponse toBlockedUserResponse(BlockedUser blockedUser) {
        if (blockedUser == null || blockedUser.getBlockedUser() == null) {
            return null;
        }
        User blocked = blockedUser.getBlockedUser();
        return new BlockedUserResponse(
                blocked.getPublicId(),
                blocked.getFullName(),
                blocked.getUsername(),
                blocked.getProfileImageUrl(),
                blocked.getCreatedAt()
        );
    }

    public static PrivacySettingsResponse toPrivacySettingsResponse(UserPrivacySetting setting) {
        if (setting == null) {
            return new PrivacySettingsResponse(
                    PrivacyVisibility.EVERYONE,
                    PrivacyVisibility.EVERYONE,
                    PrivacyVisibility.EVERYONE,
                    MessagePermission.EVERYONE
            );
        }
        return new PrivacySettingsResponse(
                setting.getProfilePhotoVisibility(),
                setting.getLastSeenVisibility(),
                setting.getOnlineStatusVisibility(),
                setting.getMessagePermission()
        );
    }
}
