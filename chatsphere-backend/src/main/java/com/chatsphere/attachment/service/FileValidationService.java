package com.chatsphere.attachment.service;

import com.chatsphere.common.enums.MessageType;
import com.chatsphere.exception.BadRequestException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class FileValidationService {

    private final Tika tika = new Tika();

    public static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;    // 10 MB
    public static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;    // 50 MB
    public static final long MAX_DOCUMENT_SIZE = 25L * 1024 * 1024; // 25 MB
    public static final long MAX_AUDIO_SIZE = 25L * 1024 * 1024;    // 25 MB

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov", "mkv");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "csv");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "ogg", "aac");

    private static final Set<String> IMAGE_MIMES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_MIMES = Set.of("video/mp4", "video/webm", "video/quicktime", "video/x-matroska");
    private static final Set<String> DOCUMENT_MIMES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/csv"
    );
    private static final Set<String> AUDIO_MIMES = Set.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/m4a", "audio/x-m4a", "audio/ogg", "audio/aac", "audio/mp4");

    public record ValidatedFileInfo(
            MessageType fileType,
            String detectedMimeType,
            String originalFileName,
            long fileSize
    ) {}

    public ValidatedFileInfo validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or not provided");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException("File name cannot be empty");
        }

        String extension = getFileExtension(originalFilename);
        if (extension.isEmpty()) {
            throw new BadRequestException("File must have a valid extension");
        }

        MessageType type = determineTypeFromExtension(extension);
        if (type == null) {
            throw new BadRequestException("Unsupported file extension: " + extension);
        }

        long size = file.getSize();
        validateSize(type, size);

        // Magic byte / real MIME type detection
        String detectedMime;
        try {
            detectedMime = tika.detect(file.getInputStream(), originalFilename);
        } catch (IOException e) {
            throw new BadRequestException("Could not determine file content type");
        }

        validateMimeType(type, detectedMime);

        return new ValidatedFileInfo(type, detectedMime, originalFilename, size);
    }

    private void validateSize(MessageType type, long size) {
        long maxSize = switch (type) {
            case IMAGE -> MAX_IMAGE_SIZE;
            case VIDEO -> MAX_VIDEO_SIZE;
            case DOCUMENT -> MAX_DOCUMENT_SIZE;
            case AUDIO -> MAX_AUDIO_SIZE;
            default -> throw new BadRequestException("Invalid attachment type");
        };

        if (size > maxSize) {
            throw new BadRequestException(
                    String.format("File size (%d bytes) exceeds maximum allowed size for %s (%d bytes)", size, type, maxSize));
        }
    }

    private void validateMimeType(MessageType type, String detectedMime) {
        boolean valid = switch (type) {
            case IMAGE -> IMAGE_MIMES.contains(detectedMime) || detectedMime.startsWith("image/");
            case VIDEO -> VIDEO_MIMES.contains(detectedMime) || detectedMime.startsWith("video/");
            case DOCUMENT -> DOCUMENT_MIMES.contains(detectedMime) || detectedMime.startsWith("text/");
            case AUDIO -> AUDIO_MIMES.contains(detectedMime) || detectedMime.startsWith("audio/");
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "File content signature (" + detectedMime + ") does not match permitted formats for " + type);
        }
    }

    private MessageType determineTypeFromExtension(String ext) {
        if (IMAGE_EXTENSIONS.contains(ext)) return MessageType.IMAGE;
        if (VIDEO_EXTENSIONS.contains(ext)) return MessageType.VIDEO;
        if (DOCUMENT_EXTENSIONS.contains(ext)) return MessageType.DOCUMENT;
        if (AUDIO_EXTENSIONS.contains(ext)) return MessageType.AUDIO;
        return null;
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx >= 0 && idx < filename.length() - 1) {
            return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
