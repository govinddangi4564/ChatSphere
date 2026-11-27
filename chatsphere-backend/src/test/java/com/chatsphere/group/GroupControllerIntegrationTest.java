package com.chatsphere.group;

import com.chatsphere.auth.dto.AuthResponse;
import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.group.dto.*;
import com.chatsphere.message.dto.CursorApiResponse;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.SendMessageRequest;
import com.chatsphere.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GroupControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String user1Token;
    private String user1PublicId;

    private String user2Token;
    private String user2PublicId;

    private String user3Token;
    private String user3PublicId;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Register & Login User 1 (Admin)
        user1PublicId = registerUser("Admin Alice", "alice_" + suffix, "alice_" + suffix + "@example.com");
        user1Token = loginUser("alice_" + suffix);

        // Register & Login User 2 (Member)
        user2PublicId = registerUser("Member Bob", "bob_" + suffix, "bob_" + suffix + "@example.com");
        user2Token = loginUser("bob_" + suffix);

        // Register & Login User 3 (Invited / Removed)
        user3PublicId = registerUser("Charlie Outsider", "charlie_" + suffix, "charlie_" + suffix + "@example.com");
        user3Token = loginUser("charlie_" + suffix);
    }

    private String registerUser(String name, String username, String email) {
        RegisterRequest req = new RegisterRequest(name, username, email, "Password123!");
        ResponseEntity<ApiResponse<UserResponse>> resp = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );
        return resp.getBody().data().publicId();
    }

    private String loginUser(String username) {
        LoginRequest req = new LoginRequest(username, "Password123!");
        ResponseEntity<ApiResponse<AuthResponse>> resp = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );
        return resp.getBody().data().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void groupLifecycleAndPermissionsTest() {
        // 1. User 1 creates group with User 2
        CreateGroupRequest createReq = new CreateGroupRequest(
                "Backend Team",
                "Backend developer team group",
                null,
                List.of(user2PublicId)
        );

        ResponseEntity<ApiResponse<GroupDetailsResponse>> createResp = restTemplate.exchange(
                "/api/groups",
                HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<GroupDetailsResponse>>() {}
        );

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        GroupDetailsResponse groupData = createResp.getBody().data();
        String groupPublicId = groupData.publicId();
        assertThat(groupData.title()).isEqualTo("Backend Team");
        assertThat(groupData.currentUserRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(groupData.members()).hasSize(2);

        // 2. User 2 (Member) gets group details
        ResponseEntity<ApiResponse<GroupDetailsResponse>> getDetailsResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user2Token)),
                new ParameterizedTypeReference<ApiResponse<GroupDetailsResponse>>() {}
        );
        assertThat(getDetailsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getDetailsResp.getBody().data().currentUserRole()).isEqualTo(MemberRole.MEMBER);

        // 3. User 3 (Non-member) getting details is forbidden (403)
        ResponseEntity<ApiResponse<GroupDetailsResponse>> outsiderResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user3Token)),
                new ParameterizedTypeReference<ApiResponse<GroupDetailsResponse>>() {}
        );
        assertThat(outsiderResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 4. User 2 (regular member) attempts to update group info -> 403 Forbidden
        UpdateGroupRequest updateReq = new UpdateGroupRequest("Hacked Title", null, null);
        ResponseEntity<ApiResponse<GroupDetailsResponse>> memberUpdateResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, authHeaders(user2Token)),
                new ParameterizedTypeReference<ApiResponse<GroupDetailsResponse>>() {}
        );
        assertThat(memberUpdateResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 5. User 1 (Admin) updates group info -> 200 OK
        UpdateGroupRequest adminUpdateReq = new UpdateGroupRequest("Core Engineering", "Updated description", null);
        ResponseEntity<ApiResponse<GroupDetailsResponse>> adminUpdateResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId,
                HttpMethod.PUT,
                new HttpEntity<>(adminUpdateReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<GroupDetailsResponse>>() {}
        );
        assertThat(adminUpdateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminUpdateResp.getBody().data().title()).isEqualTo("Core Engineering");

        // 6. User 1 (Admin) adds User 3 to the group -> 200 OK
        AddGroupMembersRequest addMembersReq = new AddGroupMembersRequest(List.of(user3PublicId));
        ResponseEntity<ApiResponse<GroupDetailsResponse>> addResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(addMembersReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<GroupDetailsResponse>>() {}
        );
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addResp.getBody().data().members()).hasSize(3);

        // 7. User 2 sends a message to the group -> 201/200 OK
        SendMessageRequest msgReq = new SendMessageRequest(
                groupPublicId,
                "client-group-msg-1",
                "Hello everyone in Core Engineering!",
                MessageType.TEXT
        );
        ResponseEntity<ApiResponse<MessageResponse>> msgResp = restTemplate.exchange(
                "/api/messages",
                HttpMethod.POST,
                new HttpEntity<>(msgReq, authHeaders(user2Token)),
                new ParameterizedTypeReference<ApiResponse<MessageResponse>>() {}
        );
        assertThat(msgResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 8. User 1 (Admin) removes User 3 -> 200 OK
        ResponseEntity<ApiResponse<Void>> removeResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId + "/members/" + user3PublicId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(removeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 9. Removed User 3 attempts to send message to group -> 403 Forbidden
        SendMessageRequest unauthorizedMsg = new SendMessageRequest(
                groupPublicId,
                "client-unauth-msg",
                "Can I still talk?",
                MessageType.TEXT
        );
        ResponseEntity<ApiResponse<MessageResponse>> unauthResp = restTemplate.exchange(
                "/api/messages",
                HttpMethod.POST,
                new HttpEntity<>(unauthorizedMsg, authHeaders(user3Token)),
                new ParameterizedTypeReference<ApiResponse<MessageResponse>>() {}
        );
        assertThat(unauthResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 10. Removed User 3 attempts to get messages from group -> 403 Forbidden
        ResponseEntity<CursorApiResponse<MessageResponse>> getMsgResp = restTemplate.exchange(
                "/api/conversations/" + groupPublicId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user3Token)),
                new ParameterizedTypeReference<CursorApiResponse<MessageResponse>>() {}
        );
        assertThat(getMsgResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 11. User 2 leaves the group -> 200 OK
        ResponseEntity<ApiResponse<Void>> leaveResp = restTemplate.exchange(
                "/api/groups/" + groupPublicId + "/leave",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(user2Token)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(leaveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
