package com.chatsphere.group.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.group.dto.*;
import com.chatsphere.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups", description = "Group creation, membership, and administration endpoints")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @Operation(summary = "Create a new group conversation")
    public ResponseEntity<ApiResponse<GroupDetailsResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Principal principal
    ) {
        GroupDetailsResponse response = groupService.createGroup(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created successfully", response));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get group details and member list")
    public ResponseEntity<ApiResponse<GroupDetailsResponse>> getGroupDetails(
            @PathVariable String publicId,
            Principal principal
    ) {
        GroupDetailsResponse response = groupService.getGroupDetails(principal.getName(), publicId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Update group information (Admins only)")
    public ResponseEntity<ApiResponse<GroupDetailsResponse>> updateGroup(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateGroupRequest request,
            Principal principal
    ) {
        GroupDetailsResponse response = groupService.updateGroup(principal.getName(), publicId, request);
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", response));
    }

    @PostMapping("/{publicId}/members")
    @Operation(summary = "Add members to group (Admins only)")
    public ResponseEntity<ApiResponse<GroupDetailsResponse>> addMembers(
            @PathVariable String publicId,
            @Valid @RequestBody AddGroupMembersRequest request,
            Principal principal
    ) {
        GroupDetailsResponse response = groupService.addMembers(principal.getName(), publicId, request);
        return ResponseEntity.ok(ApiResponse.success("Members added successfully", response));
    }

    @DeleteMapping("/{publicId}/members/{userPublicId}")
    @Operation(summary = "Remove a member from group (Admins only)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable String publicId,
            @PathVariable String userPublicId,
            Principal principal
    ) {
        groupService.removeMember(principal.getName(), publicId, userPublicId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    @PostMapping("/{publicId}/leave")
    @Operation(summary = "Leave a group")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @PathVariable String publicId,
            Principal principal
    ) {
        groupService.leaveGroup(principal.getName(), publicId);
        return ResponseEntity.ok(ApiResponse.success("Left group successfully", null));
    }

    @PutMapping("/{publicId}/admins/{userPublicId}")
    @Operation(summary = "Promote or demote an administrator (Admins only)")
    public ResponseEntity<ApiResponse<GroupDetailsResponse>> updateMemberRole(
            @PathVariable String publicId,
            @PathVariable String userPublicId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            Principal principal
    ) {
        GroupDetailsResponse response = groupService.updateMemberRole(principal.getName(), publicId, userPublicId, request);
        return ResponseEntity.ok(ApiResponse.success("Member role updated successfully", response));
    }
}
