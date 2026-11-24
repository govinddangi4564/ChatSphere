package com.chatsphere.contact;

import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.contact.dto.ContactResponse;
import com.chatsphere.contact.entity.UserContact;
import com.chatsphere.contact.repository.UserContactRepository;
import com.chatsphere.contact.service.ContactService;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ConflictException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.entity.UserPrivacySetting;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserPrivacySettingRepository;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserContactRepository userContactRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private UserPrivacySettingRepository privacySettingRepository;

    private ContactService contactService;

    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(userRepository, userContactRepository, blockedUserRepository, privacySettingRepository);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setPublicId(UUID.randomUUID().toString());
        currentUser.setUsername("alice");

        targetUser = new User();
        targetUser.setId(2L);
        targetUser.setPublicId(UUID.randomUUID().toString());
        targetUser.setUsername("bob");
        targetUser.setFullName("Bob Jones");
    }

    @Test
    void addContact_CannotAddSelf() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(currentUser.getPublicId())).willReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> contactService.addContact("alice", currentUser.getPublicId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot add yourself");
    }

    @Test
    void addContact_BlockedByTarget_ThrowsResourceNotFound() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(2L, 1L)).willReturn(true);

        assertThatThrownBy(() -> contactService.addContact("alice", targetUser.getPublicId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addContact_AlreadyExists_ThrowsConflict() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(2L, 1L)).willReturn(false);
        given(userContactRepository.existsByOwnerIdAndContactUserId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> contactService.addContact("alice", targetUser.getPublicId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in your contacts");
    }

    @Test
    void addContact_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));
        given(blockedUserRepository.existsByBlockerIdAndBlockedUserId(2L, 1L)).willReturn(false);
        given(userContactRepository.existsByOwnerIdAndContactUserId(1L, 2L)).willReturn(false);
        given(userContactRepository.save(any(UserContact.class))).willAnswer(inv -> {
            UserContact c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        ContactResponse response = contactService.addContact("alice", targetUser.getPublicId());

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("bob");
        verify(userContactRepository).save(any(UserContact.class));
    }

    @Test
    void getContacts_ReturnsListWithPrivacyCheck() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));

        UserContact contact = new UserContact();
        contact.setId(10L);
        contact.setOwner(currentUser);
        contact.setContactUser(targetUser);

        given(userContactRepository.findByOwnerIdWithContactUser(1L)).willReturn(List.of(contact));
        given(userContactRepository.existsByOwnerIdAndContactUserId(2L, 1L)).willReturn(false);

        UserPrivacySetting privacy = new UserPrivacySetting();
        privacy.setProfilePhotoVisibility(PrivacyVisibility.CONTACTS_ONLY);
        given(privacySettingRepository.findByUserId(2L)).willReturn(Optional.of(privacy));

        List<ContactResponse> contacts = contactService.getContacts("alice");

        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).username()).isEqualTo("bob");
        assertThat(contacts.get(0).profileImageUrl()).isNull(); // Privacy CONTACTS_ONLY and target hasn't added alice back
    }

    @Test
    void removeContact_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(currentUser));
        given(userRepository.findByPublicId(targetUser.getPublicId())).willReturn(Optional.of(targetUser));

        contactService.removeContact("alice", targetUser.getPublicId());

        verify(userContactRepository).deleteByOwnerIdAndContactUserId(1L, 2L);
    }
}
