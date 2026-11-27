package com.chatsphere.attachment.controller;

import com.chatsphere.attachment.dto.AttachmentResponse;
import com.chatsphere.attachment.service.AttachmentService;
import com.chatsphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachments", description = "File attachment upload, download, and deletion")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload attachment file", description = "Uploads an image, video, document, or audio file")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        AttachmentResponse response = attachmentService.uploadAttachment(file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", response));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Download or stream attachment", description = "Streams local files or redirects to cloud storage URL")
    public ResponseEntity<?> downloadAttachment(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        AttachmentService.AttachmentResourceInfo info = attachmentService.getAttachmentForDownload(publicId, authentication.getName());

        if (info.resource() != null) {
            String contentDisposition = "attachment; filename=\"" + info.fileName() + "\"";
            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(info.mimeType());
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(mediaType)
                    .contentLength(info.fileSize())
                    .body(info.resource());
        }

        // For Cloudinary or external URL redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(info.storageUrl()))
                .build();
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete attachment", description = "Deletes an attachment file and its metadata")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        attachmentService.deleteAttachment(publicId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully", null));
    }
}
