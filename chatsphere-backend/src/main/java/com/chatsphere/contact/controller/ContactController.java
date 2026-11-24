package com.chatsphere.contact.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.contact.dto.ContactResponse;
import com.chatsphere.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "Endpoints for managing user contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "List contacts", description = "Retrieves the authenticated user's contacts list")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> getContacts(@AuthenticationPrincipal UserDetails userDetails) {
        List<ContactResponse> response = contactService.getContacts(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Contacts retrieved successfully", response));
    }

    @PostMapping("/{publicId}")
    @Operation(summary = "Add contact", description = "Adds a user to the authenticated user's contacts")
    public ResponseEntity<ApiResponse<ContactResponse>> addContact(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId
    ) {
        ContactResponse response = contactService.addContact(userDetails.getUsername(), publicId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact added successfully", response));
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Remove contact", description = "Removes a user from contacts")
    public ResponseEntity<ApiResponse<Void>> removeContact(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId
    ) {
        contactService.removeContact(userDetails.getUsername(), publicId);
        return ResponseEntity.ok(ApiResponse.success("Contact removed successfully", null));
    }
}
