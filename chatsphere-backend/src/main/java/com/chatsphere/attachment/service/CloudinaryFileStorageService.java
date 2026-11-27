package com.chatsphere.attachment.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.chatsphere.exception.AppException;
import com.chatsphere.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public class CloudinaryFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileStorageService.class);

    private final Cloudinary cloudinary;

    public CloudinaryFileStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            ));

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String resourceType = (String) uploadResult.get("resource_type");

            String thumbnailUrl = null;
            if ("image".equalsIgnoreCase(resourceType)) {
                thumbnailUrl = cloudinary.url()
                        .transformation(new com.cloudinary.Transformation<>().width(300).height(300).crop("fill"))
                        .generate(publicId);
            }

            return new StoredFile(
                    publicId,
                    secureUrl,
                    thumbnailUrl,
                    originalFilename,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new AppException("Failed to upload file to Cloudinary");
        }
    }

    @Override
    public void delete(String storagePublicId) {
        try {
            cloudinary.uploader().destroy(storagePublicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary file with publicId: {}", storagePublicId, e);
        }
    }

    @Override
    public Resource loadAsResource(String storagePublicId) {
        throw new BadRequestException("Direct resource streaming is only applicable for local storage");
    }
}
