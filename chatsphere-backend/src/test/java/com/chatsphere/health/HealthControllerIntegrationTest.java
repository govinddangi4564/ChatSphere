package com.chatsphere.health;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.health.dto.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetHealthEndpointReturnsSuccess() {
        ResponseEntity<ApiResponse<HealthResponse>> response = restTemplate.exchange(
                "/api/health",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<HealthResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("ChatSphere API is operational");
        assertThat(response.getBody().data()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo("UP");
        assertThat(response.getBody().data().database()).isEqualTo("UP");
        assertThat(response.getBody().data().version()).isEqualTo("1.0.0");
    }
}
