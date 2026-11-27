package com.chatsphere.attachment.service;

import com.chatsphere.common.enums.MessageType;
import com.chatsphere.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationServiceTest {

    private FileValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new FileValidationService();
    }

    @Test
    void validate_validJpegImage_succeeds() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);

        FileValidationService.ValidatedFileInfo info = validationService.validate(file);

        assertNotNull(info);
        assertEquals(MessageType.IMAGE, info.fileType());
        assertEquals("photo.jpg", info.originalFileName());
    }

    @Test
    void validate_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        assertThrows(BadRequestException.class, () -> validationService.validate(file));
    }

    @Test
    void validate_unsupportedExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "dangerous.exe", "application/octet-stream", "MZ...".getBytes());

        assertThrows(BadRequestException.class, () -> validationService.validate(file));
    }

    @Test
    void validate_spoofedExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "This is plain text pretending to be JPEG".getBytes());

        assertThrows(BadRequestException.class, () -> validationService.validate(file));
    }

    @Test
    void validate_oversizeImage_throwsException() {
        byte[] largeBytes = new byte[(int) (FileValidationService.MAX_IMAGE_SIZE + 1024)];
        largeBytes[0] = (byte) 0xFF;
        largeBytes[1] = (byte) 0xD8;
        largeBytes[2] = (byte) 0xFF;
        largeBytes[3] = (byte) 0xE0;

        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);

        assertThrows(BadRequestException.class, () -> validationService.validate(file));
    }

    @Test
    void validate_validPdfDocument_succeeds() {
        byte[] pdfBytes = "%PDF-1.4 header content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", pdfBytes);

        FileValidationService.ValidatedFileInfo info = validationService.validate(file);

        assertNotNull(info);
        assertEquals(MessageType.DOCUMENT, info.fileType());
    }
}
