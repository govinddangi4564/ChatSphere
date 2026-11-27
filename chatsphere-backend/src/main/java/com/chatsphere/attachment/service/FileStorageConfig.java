package com.chatsphere.attachment.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
public class FileStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    @Primary
    public FileStorageService fileStorageService(LocalFileStorageService localStorageService) {
        if (StringUtils.hasText(cloudName) && !"placeholder".equalsIgnoreCase(cloudName) &&
            StringUtils.hasText(apiKey) && !"placeholder".equalsIgnoreCase(apiKey) &&
            StringUtils.hasText(apiSecret) && !"placeholder".equalsIgnoreCase(apiSecret)) {

            log.info("Configuring Cloudinary storage with cloud name: {}", cloudName);
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
            return new CloudinaryFileStorageService(cloudinary);
        }

        log.info("Cloudinary credentials not configured or placeholder detected; falling back to LocalFileStorageService");
        return localStorageService;
    }
}
