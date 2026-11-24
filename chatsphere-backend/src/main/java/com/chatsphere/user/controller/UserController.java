package com.chatsphere.user.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.user.dto.*;
import com.chatsphere.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for user profile, search, blocking, and privacy settings")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the authenticated user's full profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates full name, bio, and profile image URL (Note: Real file uploads to cloud storage arrive in Phase 7)")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Paginated case-insensitive user search with sort whitelist (username, fullName), excluding caller and blocked users")
    public ResponseEntity<ApiResponse<PageResponse<UserPublicProfileResponse>>> searchUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "query", defaultValue = "") String query,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        PageResponse<UserPublicProfileResponse> response = userService.searchUsers(userDetails.getUsername(), query, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get user public profile", description = "Retrieves a user's public profile respecting their privacy visibility settings")
    public ResponseEntity<ApiResponse<UserPublicProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId
    ) {
        UserPublicProfileResponse response = userService.getUserProfile(userDetails.getUsername(), publicId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    @PostMapping("/{publicId}/block")
    @Operation(summary = "Block user", description = "Blocks a user by their publicId")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId
    ) {
        userService.blockUser(userDetails.getUsername(), publicId);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/{publicId}/block")
    @Operation(summary = "Unblock user", description = "Unblocks a previously blocked user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId
    ) {
        userService.unblockUser(userDetails.getUsername(), publicId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @GetMapping("/blocked")
    @Operation(summary = "List blocked users", description = "Retrieves all users blocked by the authenticated user")
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> getBlockedUsers(@AuthenticationPrincipal UserDetails userDetails) {
        List<BlockedUserResponse> response = userService.getBlockedUsers(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Blocked users retrieved successfully", response));
    }

    @GetMapping("/privacy")
    @Operation(summary = "Get privacy settings", description = "Retrieves the authenticated user's privacy visibility preferences")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> getPrivacySettings(@AuthenticationPrincipal UserDetails userDetails) {
        PrivacySettingsResponse response = userService.getPrivacySettings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Privacy settings retrieved successfully", response));
    }

    @PutMapping("/privacy")
    @Operation(summary = "Update privacy settings", description = "Updates profile photo, last seen, online status, and messaging permissions")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> updatePrivacySettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PrivacySettingsRequest request
    ) {
        PrivacySettingsResponse response = userService.updatePrivacySettings(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Privacy settings updated successfully", response));
    }

    @PostMapping("/presence/heartbeat")
    @Operation(summary = "Heartbeat last seen", description = "Updates the authenticated user's last seen timestamp")
    public ResponseEntity<ApiResponse<Void>> heartbeat(@AuthenticationPrincipal UserDetails userDetails) {
        userService.updateLastSeen(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Presence updated", null));
    }
}
