package com.chatsphere.conversation;

import com.chatsphere.auth.dto.AuthResponse;
import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.conversation.dto.ArchiveRequest;
import com.chatsphere.conversation.dto.ConversationDetailsResponse;
import com.chatsphere.conversation.dto.ConversationSummaryResponse;
import com.chatsphere.conversation.dto.CreatePrivateConversationRequest;
import com.chatsphere.conversation.dto.MuteRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConversationControllerIntegrationTest {

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

        // Register & Login User 1
        user1PublicId = registerUser("User One", "user1_" + suffix, "user1_" + suffix + "@example.com");
        user1Token = loginUser("user1_" + suffix);

        // Register & Login User 2
        user2PublicId = registerUser("User Two", "user2_" + suffix, "user2_" + suffix + "@example.com");
        user2Token = loginUser("user2_" + suffix);

        // Register & Login User 3
        user3PublicId = registerUser("User Three", "user3_" + suffix, "user3_" + suffix + "@example.com");
        user3Token = loginUser("user3_" + suffix);
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
    void conversationLifecycleAndSecurityTests() {
        // 1. User 1 creates conversation with User 2
        CreatePrivateConversationRequest createReq = new CreatePrivateConversationRequest(user2PublicId);
        ResponseEntity<ApiResponse<ConversationDetailsResponse>> createResp1 = restTemplate.exchange(
                "/api/conversations/private",
                HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<ConversationDetailsResponse>>() {}
        );

        assertThat(createResp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp1.getBody().data()).isNotNull();
        String convId = createResp1.getBody().data().publicId();
        assertThat(convId).isNotBlank();

        // 2. Duplicate Prevention: User 1 calls create again with User 2
        ResponseEntity<ApiResponse<ConversationDetailsResponse>> createResp2 = restTemplate.exchange(
                "/api/conversations/private",
                HttpMethod.POST,
                new HttpEntity<>(createReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<ConversationDetailsResponse>>() {}
        );

        assertThat(createResp2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp2.getBody().data().publicId()).isEqualTo(convId); // Same conversation returned!

        // 3. User 3 attempts to access User 1 & 2's conversation (Unauthorized/Not a member)
        ResponseEntity<ApiResponse<ConversationDetailsResponse>> unauthorizedResp = restTemplate.exchange(
                "/api/conversations/" + convId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user3Token)),
                new ParameterizedTypeReference<ApiResponse<ConversationDetailsResponse>>() {}
        );
        assertThat(unauthorizedResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 4. User 1 lists conversations
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> listResp = restTemplate.exchange(
                "/api/conversations?page=0&size=20&sort=lastMessageAt,desc",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody().data().content()).hasSize(1);
        assertThat(listResp.getBody().data().content().get(0).publicId()).isEqualTo(convId);
        assertThat(listResp.getBody().data().content().get(0).participant().publicId()).isEqualTo(user2PublicId);

        // 5. User 1 archives conversation
        ArchiveRequest archiveReq = new ArchiveRequest(true);
        ResponseEntity<ApiResponse<Void>> archiveResp = restTemplate.exchange(
                "/api/conversations/" + convId + "/archive",
                HttpMethod.PUT,
                new HttpEntity<>(archiveReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(archiveResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify filter ?archived=true
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> archivedList = restTemplate.exchange(
                "/api/conversations?archived=true",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        assertThat(archivedList.getBody().data().content()).hasSize(1);

        // Verify filter ?archived=false
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> unarchivedList = restTemplate.exchange(
                "/api/conversations?archived=false",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        assertThat(unarchivedList.getBody().data().content()).isEmpty();

        // 6. User 1 mutes conversation
        MuteRequest muteReq = new MuteRequest(true);
        ResponseEntity<ApiResponse<Void>> muteResp = restTemplate.exchange(
                "/api/conversations/" + convId + "/mute",
                HttpMethod.PUT,
                new HttpEntity<>(muteReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(muteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. Sort Whitelist Validation: invalid sort field returns 400 Bad Request
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> invalidSortResp = restTemplate.exchange(
                "/api/conversations?sort=passwordHash,asc",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        assertThat(invalidSortResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 8. Pagination Limits: page size > 50 returns 400 Bad Request
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> excessiveSizeResp = restTemplate.exchange(
                "/api/conversations?size=51",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        assertThat(excessiveSizeResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
