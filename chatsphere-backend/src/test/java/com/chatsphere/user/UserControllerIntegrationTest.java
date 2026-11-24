package com.chatsphere.user;

import com.chatsphere.auth.dto.AuthResponse;
import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.user.dto.*;
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
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String user1Token;
    private String user1PublicId;

    private String user2Token;
    private String user2PublicId;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Register user 1
        RegisterRequest reg1 = new RegisterRequest("User One", "user1_" + suffix, "user1_" + suffix + "@example.com", "Password123!");
        ResponseEntity<ApiResponse<UserResponse>> resp1 = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(reg1),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );
        user1PublicId = resp1.getBody().data().publicId();

        // Login user 1
        LoginRequest login1 = new LoginRequest("user1_" + suffix, "Password123!");
        ResponseEntity<ApiResponse<AuthResponse>> loginResp1 = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(login1),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );
        user1Token = loginResp1.getBody().data().accessToken();

        // Register user 2
        RegisterRequest reg2 = new RegisterRequest("User Two", "user2_" + suffix, "user2_" + suffix + "@example.com", "Password123!");
        ResponseEntity<ApiResponse<UserResponse>> resp2 = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(reg2),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );
        user2PublicId = resp2.getBody().data().publicId();

        // Login user 2
        LoginRequest login2 = new LoginRequest("user2_" + suffix, "Password123!");
        ResponseEntity<ApiResponse<AuthResponse>> loginResp2 = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(login2),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );
        user2Token = loginResp2.getBody().data().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void getMe_And_UpdateProfile() {
        // GET /api/users/me
        ResponseEntity<ApiResponse<UserResponse>> meResp = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody().data().publicId()).isEqualTo(user1PublicId);

        // PUT /api/users/me
        UpdateProfileRequest updateReq = new UpdateProfileRequest("Updated One", "Hello bio", "https://img.com/p.jpg");
        ResponseEntity<ApiResponse<UserResponse>> updateResp = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResp.getBody().data().fullName()).isEqualTo("Updated One");
        assertThat(updateResp.getBody().data().bio()).isEqualTo("Hello bio");
        assertThat(updateResp.getBody().data().profileImageUrl()).isEqualTo("https://img.com/p.jpg");
    }

    @Test
    void searchUsers_ExcludesCurrentUser() {
        ResponseEntity<ApiResponse<PageResponse<UserPublicProfileResponse>>> searchResp = restTemplate.exchange(
                "/api/users/search?query=User&sort=username,asc",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PageResponse<UserPublicProfileResponse>>>() {}
        );

        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<UserPublicProfileResponse> users = searchResp.getBody().data().content();
        assertThat(users.stream().anyMatch(u -> u.publicId().equals(user1PublicId))).isFalse();
        assertThat(users.stream().anyMatch(u -> u.publicId().equals(user2PublicId))).isTrue();
    }

    @Test
    void blockAndUnblockUser() {
        // Block user 2
        ResponseEntity<ApiResponse<Void>> blockResp = restTemplate.exchange(
                "/api/users/" + user2PublicId + "/block",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(blockResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify user 2 in blocked list
        ResponseEntity<ApiResponse<List<BlockedUserResponse>>> blockedListResp = restTemplate.exchange(
                "/api/users/blocked",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<List<BlockedUserResponse>>>() {}
        );
        assertThat(blockedListResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(blockedListResp.getBody().data().stream().anyMatch(b -> b.publicId().equals(user2PublicId))).isTrue();

        // User 2 cannot find User 1 profile anymore
        ResponseEntity<ApiResponse<UserPublicProfileResponse>> user1By2 = restTemplate.exchange(
                "/api/users/" + user1PublicId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user2Token)),
                new ParameterizedTypeReference<ApiResponse<UserPublicProfileResponse>>() {}
        );
        assertThat(user1By2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Unblock user 2
        ResponseEntity<ApiResponse<Void>> unblockResp = restTemplate.exchange(
                "/api/users/" + user2PublicId + "/block",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(unblockResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void privacySettings_Lifecycle() {
        // Update privacy settings
        PrivacySettingsRequest req = new PrivacySettingsRequest(
                PrivacyVisibility.CONTACTS_ONLY,
                PrivacyVisibility.NOBODY,
                PrivacyVisibility.EVERYONE,
                MessagePermission.CONTACTS_ONLY
        );

        ResponseEntity<ApiResponse<PrivacySettingsResponse>> updateResp = restTemplate.exchange(
                "/api/users/privacy",
                HttpMethod.PUT,
                new HttpEntity<>(req, authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PrivacySettingsResponse>>() {}
        );
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResp.getBody().data().profilePhotoVisibility()).isEqualTo(PrivacyVisibility.CONTACTS_ONLY);

        // Get privacy settings
        ResponseEntity<ApiResponse<PrivacySettingsResponse>> getResp = restTemplate.exchange(
                "/api/users/privacy",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(user1Token)),
                new ParameterizedTypeReference<ApiResponse<PrivacySettingsResponse>>() {}
        );
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().data().lastSeenVisibility()).isEqualTo(PrivacyVisibility.NOBODY);
    }
}
