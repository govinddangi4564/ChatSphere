package com.chatsphere.attachment.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile upload(MultipartFile file, String folder);
    void delete(String storagePublicId);
    Resource loadAsResource(String storagePublicId);
}
