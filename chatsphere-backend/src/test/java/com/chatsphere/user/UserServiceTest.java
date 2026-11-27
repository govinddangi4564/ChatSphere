package com.chatsphere.user;

import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.contact.repository.UserContactRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.user.dto.PrivacySettingsRequest;
import com.chatsphere.user.dto.PrivacySettingsResponse;
import com.chatsphere.user.dto.UpdateProfileRequest;
import com.chatsphere.user.dto.UserPublicProfileResponse;
import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.BlockedUser;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.entity.UserPrivacySetting;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserPrivacySettingRepository;
import com.chatsphere.user.repository.UserRepository;
import com.chatsphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private UserContactRepository userContactRepository;

    @Mock
    private UserPrivacySettingRepository privacySettingRepository;

    @Mock
    private com.chatsphere.cache.service.RedisCacheService redisCacheService;

    private UserService userService;

    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, blockedUserRepository, userContactRepository, privacySettingRepository, redisCacheService);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setPublicId(UUID.randomUUID().toString());
        currentUser.setUsername("alice");
        currentUser.setFullName("Alice Smith");
        currentUser.setEmail("alice@example.com");

        targetUser = new User();
        targetUser.setId(2L);
        targetUser.setPublicId(UUID.randomUUID().toString());
        targetUser.setUsername("bob");
        targetUser.setFullName("Bob Jones");
        targetUser.setEmail("bob@example.com");
        targetUser.setProfileImageUrl("https://example.com/bob.jpg");
    }

    @Test
    void getMe_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(redisCacheService.getOrCompute(anyString(), any(), eq(UserResponse.class), any()))
                .willAnswer(inv -> {
                    java.util.function.Supplier<?> s = inv.getArgument(3);
                    return s.get();
                });

        UserResponse response = userService.getMe("alice");

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.fullName()).isEqualTo("Alice Smith");
    }

    @Test
    void updateProfile_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Alice Updated", "New Bio", "https://img.com/alice.png");
        UserResponse response = userService.updateProfile("alice", request);

        assertThat(response.fullName()).isEqualTo("Alice Updated");
        assertThat(response.bio()).isEqualTo("New Bio");
        assertThat(response.profileImageUrl()).isEqualTo("https://img.com/alice.png");
    }

    @Test
    void getUserProfile_PrivacyEveryone_ExposesFields() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(1L, 2L)).willReturn(false);
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(2L, 1L)).willReturn(false);
        given(userContactRepository.existsByOwnerIdAndContactUserId(1L, 2L)).willReturn(false);
        given(userContactRepository.existsByOwnerIdAndContactUserId(2L, 1L)).willReturn(false);

        UserPrivacySetting privacy = new UserPrivacySetting();
        privacy.setProfilePhotoVisibility(PrivacyVisibility.EVERYONE);
        privacy.setLastSeenVisibility(PrivacyVisibility.EVERYONE);
        given(privacySettingRepository.findByUserId(2L)).willReturn(Optional.of(privacy));

        UserPublicProfileResponse response = userService.getUserProfile("alice", targetUser.getPublicId());

        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/bob.jpg");
        assertThat(response.isContact()).isFalse();
        assertThat(response.isBlocked()).isFalse();
    }

    @Test
    void getUserProfile_PrivacyContactsOnly_NotContact_HidesFields() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(1L, 2L)).willReturn(false);
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(2L, 1L)).willReturn(false);
        given(userContactRepository.existsByOwnerIdAndContactUserId(1L, 2L)).willReturn(false);
        given(userContactRepository.existsByOwnerIdAndContactUserId(2L, 1L)).willReturn(false);

        UserPrivacySetting privacy = new UserPrivacySetting();
        privacy.setProfilePhotoVisibility(PrivacyVisibility.CONTACTS_ONLY);
        privacy.setLastSeenVisibility(PrivacyVisibility.CONTACTS_ONLY);
        given(privacySettingRepository.findByUserId(2L)).willReturn(Optional.of(privacy));

        UserPublicProfileResponse response = userService.getUserProfile("alice", targetUser.getPublicId());

        assertThat(response.profileImageUrl()).isNull();
    }

    @Test
    void getUserProfile_BlockedByTarget_ThrowsResourceNotFound() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(2L, 1L)).willReturn(true);

        assertThatThrownBy(() -> userService.getUserProfile("alice", targetUser.getPublicId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchUsers_SortValidation_InvalidSortThrowsBadRequest() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        Pageable invalidPageable = PageRequest.of(0, 10, Sort.by("passwordHash"));

        assertThatThrownBy(() -> userService.searchUsers("alice", "test", invalidPageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    void searchUsers_WildcardEscaping() {
        assertThat(userService.escapeSqlWildcards("50%_off\\deal"))
                .isEqualTo("50\\%\\_off\\\\deal");
    }

    @Test
    void searchUsers_ExcludesBlockedUsers() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(blockedUserRepository.findBlockedUserIdsByBlockerId(1L)).willReturn(List.of(2L));
        given(blockedUserRepository.findBlockerIdsByBlockedUserId(1L)).willReturn(Collections.emptyList());

        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        given(userRepository.searchUsersExcluding(eq(1L), anyCollection(), eq("bob"), any(Pageable.class)))
                .willReturn(emptyPage);

        PageResponse<UserPublicProfileResponse> result = userService.searchUsers("alice", "bob", PageRequest.of(0, 20));
        assertThat(result.content()).isEmpty();
        verify(userRepository).searchUsersExcluding(eq(1L), anyCollection(), eq("bob"), any(Pageable.class));
    }

    @Test
    void blockUser_CannotBlockSelf() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(currentUser.getPublicId())).willReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> userService.blockUser("alice", currentUser.getPublicId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot block yourself");
    }

    @Test
    void blockUser_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(1L, 2L)).willReturn(false);

        userService.blockUser("alice", targetUser.getPublicId());

        verify(blockedUserRepository).save(any(BlockedUser.class));
    }

    @Test
    void unblockUser_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));

        userService.unblockUser("alice", targetUser.getPublicId());

        verify(blockedUserRepository).deleteByBlockerIdAndBlockedUserId(1L, 2L);
    }

    @Test
    void updatePrivacySettings_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(privacySettingRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(privacySettingRepository.save(any(UserPrivacySetting.class))).willAnswer(inv -> inv.getArgument(0));

        PrivacySettingsRequest request = new PrivacySettingsRequest(
                PrivacyVisibility.CONTACTS_ONLY,
                PrivacyVisibility.NOBODY,
                PrivacyVisibility.EVERYONE,
                MessagePermission.CONTACTS_ONLY
        );

        PrivacySettingsResponse response = userService.updatePrivacySettings("alice", request);

        assertThat(response.profilePhotoVisibility()).isEqualTo(PrivacyVisibility.CONTACTS_ONLY);
        assertThat(response.lastSeenVisibility()).isEqualTo(PrivacyVisibility.NOBODY);
        assertThat(response.messagePermission()).isEqualTo(MessagePermission.CONTACTS_ONLY);
    }
}
