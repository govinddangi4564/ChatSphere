package com.chatsphere.user.mapper;

import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.User;

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
}
