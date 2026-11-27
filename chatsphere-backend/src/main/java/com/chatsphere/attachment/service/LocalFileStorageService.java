package com.chatsphere.attachment.service;

import com.chatsphere.exception.AppException;
import com.chatsphere.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service("localStorageService")
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path storageDirectory;

    public LocalFileStorageService(@Value("${app.storage.local-dir:./uploads/attachments}") String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            log.error("Could not create local upload directory at: {}", this.storageDirectory, e);
            throw new AppException("Could not initialize local storage");
        }
    }

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String cleanName = Paths.get(originalFilename).getFileName().toString();
        String fileExtension = "";
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex >= 0) {
            fileExtension = cleanName.substring(dotIndex);
        }

        String storagePublicId = UUID.randomUUID() + fileExtension;
        Path targetLocation = this.storageDirectory.resolve(storagePublicId).normalize();

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            String storageUrl = "/api/attachments/" + storagePublicId;
            return new StoredFile(
                    storagePublicId,
                    storageUrl,
                    null,
                    cleanName,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            log.error("Failed to store file {}", cleanName, e);
            throw new AppException("Failed to store file locally");
        }
    }

    @Override
    public void delete(String storagePublicId) {
        try {
            Path targetLocation = this.storageDirectory.resolve(storagePublicId).normalize();
            Files.deleteIfExists(targetLocation);
        } catch (IOException e) {
            log.warn("Failed to delete local file {}", storagePublicId, e);
        }
    }

    @Override
    public Resource loadAsResource(String storagePublicId) {
        try {
            Path targetLocation = this.storageDirectory.resolve(storagePublicId).normalize();
            Resource resource = new UrlResource(targetLocation.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Attachment file", "storagePublicId", storagePublicId);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Attachment file", "path", storagePublicId);
        }
    }
}
