package com.chatsphere.attachment.service;

import com.chatsphere.attachment.dto.AttachmentResponse;
import com.chatsphere.attachment.entity.MessageAttachment;
import com.chatsphere.attachment.repository.MessageAttachmentRepository;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.exception.AppException;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.message.entity.Message;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private MessageAttachmentRepository attachmentRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private UserRepository userRepository;

    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(
                fileStorageService,
                fileValidationService,
                attachmentRepository,
                conversationMemberRepository,
                userRepository
        );
    }

    @Test
    void uploadAttachment_success() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        FileValidationService.ValidatedFileInfo fileInfo = new FileValidationService.ValidatedFileInfo(
                MessageType.IMAGE, "image/png", "test.png", 3L
        );
        StoredFile storedFile = new StoredFile("storage-123", "/api/attachments/storage-123", null, "test.png", "image/png", 3L);

        when(fileValidationService.validate(file)).thenReturn(fileInfo);
        when(fileStorageService.upload(file, "chatsphere/attachments")).thenReturn(storedFile);
        when(attachmentRepository.save(any(MessageAttachment.class))).thenAnswer(inv -> {
            MessageAttachment att = inv.getArgument(0);
            att.setId(1L);
            att.setCreatedAt(Instant.now());
            return att;
        });

        AttachmentResponse response = attachmentService.uploadAttachment(file, "testuser");

        assertNotNull(response);
        assertEquals("test.png", response.fileName());
        assertEquals(MessageType.IMAGE, response.fileType());
        verify(attachmentRepository).save(any(MessageAttachment.class));
    }

    @Test
    void uploadAttachment_dbSaveFails_cleansUpStorageFile() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        FileValidationService.ValidatedFileInfo fileInfo = new FileValidationService.ValidatedFileInfo(
                MessageType.IMAGE, "image/png", "test.png", 3L
        );
        StoredFile storedFile = new StoredFile("storage-cleanup-id", "/api/attachments/storage-cleanup-id", null, "test.png", "image/png", 3L);

        when(fileValidationService.validate(file)).thenReturn(fileInfo);
        when(fileStorageService.upload(file, "chatsphere/attachments")).thenReturn(storedFile);
        when(attachmentRepository.save(any(MessageAttachment.class))).thenThrow(new RuntimeException("DB Connection Down"));

        assertThrows(AppException.class, () -> attachmentService.uploadAttachment(file, "testuser"));

        verify(fileStorageService).delete("storage-cleanup-id");
    }

    @Test
    void getAttachmentForDownload_unauthorizedMember_throwsForbidden() {
        User user = new User();
        user.setId(999L);
        user.setUsername("unauthorizedUser");

        when(userRepository.findByUsername("unauthorizedUser")).thenReturn(Optional.of(user));

        User sender = new User();
        sender.setId(200L);

        Conversation conv = new Conversation();
        conv.setId(50L);

        Message message = new Message();
        message.setId(10L);
        message.setSender(sender);
        message.setConversation(conv);

        MessageAttachment att = MessageAttachment.builder()
                .id(1L)
                .publicId("att-uuid-1")
                .storagePublicId("store-1")
                .fileName("secret.pdf")
                .message(message)
                .build();

        when(attachmentRepository.findByPublicId("att-uuid-1")).thenReturn(Optional.of(att));
        when(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(50L, 999L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> attachmentService.getAttachmentForDownload("att-uuid-1", "unauthorizedUser"));
    }
}
