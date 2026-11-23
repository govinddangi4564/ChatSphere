package com.chatsphere.auth;

import com.chatsphere.auth.dto.AuthResponse;
import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.common.dto.ApiResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void fullAuthLifecycleTest() {
        // 1. Register User
        RegisterRequest registerReq = new RegisterRequest(
                "John Doe",
                "johndoe",
                "john.doe@example.com",
                "Password123!"
        );

        ResponseEntity<ApiResponse<UserResponse>> registerResp = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerReq),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).isNotNull();
        assertThat(registerResp.getBody().success()).isTrue();
        assertThat(registerResp.getBody().data().username()).isEqualTo("johndoe");
        assertThat(registerResp.getBody().data().email()).isEqualTo("john.doe@example.com");
        assertThat(registerResp.getBody().data().publicId()).isNotBlank();

        // 2. Login User
        LoginRequest loginReq = new LoginRequest("johndoe", "Password123!");
        ResponseEntity<ApiResponse<AuthResponse>> loginResp = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginReq),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).isNotNull();
        assertThat(loginResp.getBody().success()).isTrue();
        String accessToken = loginResp.getBody().data().accessToken();
        assertThat(accessToken).isNotBlank();

        List<String> cookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).isNotEmpty();
        String refreshCookieHeader = cookies.stream()
                .filter(c -> c.contains("chatsphere_refresh_token="))
                .findFirst()
                .orElse(null);
        assertThat(refreshCookieHeader).isNotNull();
        assertThat(refreshCookieHeader).contains("HttpOnly");

        // Extract cookie value
        String cookieValue = refreshCookieHeader.split(";")[0];

        // 3. Access /api/health with JWT Bearer Token
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<String> healthResp = restTemplate.exchange(
                "/api/health",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                String.class
        );
        assertThat(healthResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Refresh Token via Cookie
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, cookieValue);
        ResponseEntity<ApiResponse<AuthResponse>> refreshResp = restTemplate.exchange(
                "/api/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshHeaders),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResp.getBody()).isNotNull();
        assertThat(refreshResp.getBody().success()).isTrue();
        String newAccessToken = refreshResp.getBody().data().accessToken();
        assertThat(newAccessToken).isNotBlank();

        // 5. Verify old refresh token is rejected after rotation (returns 401)
        ResponseEntity<String> oldTokenResp = restTemplate.exchange(
                "/api/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshHeaders),
                String.class
        );
        assertThat(oldTokenResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 6. Logout with new cookie
        List<String> newCookies = refreshResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        String newCookieValue = newCookies.stream()
                .filter(c -> c.contains("chatsphere_refresh_token="))
                .findFirst()
                .orElse("")
                .split(";")[0];

        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.add(HttpHeaders.COOKIE, newCookieValue);
        ResponseEntity<ApiResponse<Void>> logoutResp = restTemplate.exchange(
                "/api/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutHeaders),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logoutResp.getBody().message()).isEqualTo("Logged out successfully");
    }
}
