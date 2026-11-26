package com.chatsphere.message;

import com.chatsphere.auth.dto.AuthResponse;
import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.dto.ConversationDetailsResponse;
import com.chatsphere.conversation.dto.ConversationSummaryResponse;
import com.chatsphere.conversation.dto.CreatePrivateConversationRequest;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.message.dto.CursorApiResponse;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.SendMessageRequest;
import com.chatsphere.message.entity.Message;
import com.chatsphere.message.repository.MessageRepository;
import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MessageControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    private String user1Token;
    private String user1PublicId;
    private String user1Username;

    private String user2Token;
    private String user2PublicId;
    private String user2Username;

    private String user3Token;
    private String user3PublicId;
    private String user3Username;

    private String conversationPublicId;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        user1Username = "user1_" + suffix;
        user1PublicId = registerUser("User One", user1Username, user1Username + "@example.com");
        user1Token = loginUser(user1Username);

        user2Username = "user2_" + suffix;
        user2PublicId = registerUser("User Two", user2Username, user2Username + "@example.com");
        user2Token = loginUser(user2Username);

        user3Username = "user3_" + suffix;
        user3PublicId = registerUser("User Three", user3Username, user3Username + "@example.com");
        user3Token = loginUser(user3Username);

        // Create a private conversation between user 1 and user 2
        HttpHeaders headers = createAuthHeaders(user1Token);
        CreatePrivateConversationRequest convReq = new CreatePrivateConversationRequest(user2PublicId);
        ResponseEntity<ApiResponse<ConversationDetailsResponse>> convResp = restTemplate.exchange(
                "/api/conversations/private",
                HttpMethod.POST,
                new HttpEntity<>(convReq, headers),
                new ParameterizedTypeReference<ApiResponse<ConversationDetailsResponse>>() {}
        );
        conversationPublicId = convResp.getBody().data().publicId();
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

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void testMessageLifecycle_Send_Duplicate_NonMember_UnreadCounts_And_CursorPagination() {
        HttpHeaders user1Headers = createAuthHeaders(user1Token);
        HttpHeaders user2Headers = createAuthHeaders(user2Token);
        HttpHeaders user3Headers = createAuthHeaders(user3Token);

        // =================================================================
        // 1. Send Message: User 1 sends message to User 2
        // =================================================================
        String clientMsgId1 = UUID.randomUUID().toString();
        SendMessageRequest sendReq1 = new SendMessageRequest(
                conversationPublicId,
                clientMsgId1,
                "Hello User 2!",
                MessageType.TEXT
        );

        ResponseEntity<ApiResponse<MessageResponse>> sendResp1 = restTemplate.exchange(
                "/api/messages",
                HttpMethod.POST,
                new HttpEntity<>(sendReq1, user1Headers),
                new ParameterizedTypeReference<ApiResponse<MessageResponse>>() {}
        );

        assertThat(sendResp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sendResp1.getBody()).isNotNull();
        assertThat(sendResp1.getBody().success()).isTrue();
        MessageResponse msg1 = sendResp1.getBody().data();
        assertThat(msg1.content()).isEqualTo("Hello User 2!");
        assertThat(msg1.senderUsername()).isEqualTo(user1Username);
        assertThat(msg1.clientMessageId()).isEqualTo(clientMsgId1);

        // =================================================================
        // 2. Verify Conversation Summary: unread counts and preview
        // =================================================================
        // User 2's view: unread count should be 1, lastMessagePreview should be "Hello User 2!"
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> convsUser2 = restTemplate.exchange(
                "/api/conversations",
                HttpMethod.GET,
                new HttpEntity<>(user2Headers),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        ConversationSummaryResponse user2Summary = convsUser2.getBody().data().content().stream()
                .filter(c -> c.publicId().equals(conversationPublicId))
                .findFirst()
                .orElseThrow();
        assertThat(user2Summary.unreadCount()).isEqualTo(1);
        assertThat(user2Summary.lastMessagePreview()).isEqualTo("Hello User 2!");
        assertThat(user2Summary.lastMessageAt()).isNotNull();

        // User 1's view: unread count should be 0
        ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> convsUser1 = restTemplate.exchange(
                "/api/conversations",
                HttpMethod.GET,
                new HttpEntity<>(user1Headers),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        ConversationSummaryResponse user1Summary = convsUser1.getBody().data().content().stream()
                .filter(c -> c.publicId().equals(conversationPublicId))
                .findFirst()
                .orElseThrow();
        assertThat(user1Summary.unreadCount()).isEqualTo(0);
        assertThat(user1Summary.lastMessagePreview()).isEqualTo("Hello User 2!");

        // =================================================================
        // 3. Duplicate clientMessageId Idempotency: User 1 sends again with same clientMsgId1
        // =================================================================
        ResponseEntity<ApiResponse<MessageResponse>> sendRespDup = restTemplate.exchange(
                "/api/messages",
                HttpMethod.POST,
                new HttpEntity<>(sendReq1, user1Headers),
                new ParameterizedTypeReference<ApiResponse<MessageResponse>>() {}
        );
        assertThat(sendRespDup.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sendRespDup.getBody().data().publicId()).isEqualTo(msg1.publicId());

        // Check user 2 unread count did not increment again (still 1)
        convsUser2 = restTemplate.exchange(
                "/api/conversations",
                HttpMethod.GET,
                new HttpEntity<>(user2Headers),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        user2Summary = convsUser2.getBody().data().content().stream()
                .filter(c -> c.publicId().equals(conversationPublicId))
                .findFirst()
                .orElseThrow();
        assertThat(user2Summary.unreadCount()).isEqualTo(1);

        // =================================================================
        // 4. Non-member Rejected: User 3 tries to send and read messages
        // =================================================================
        SendMessageRequest unauthSend = new SendMessageRequest(
                conversationPublicId,
                UUID.randomUUID().toString(),
                "Intruder message",
                MessageType.TEXT
        );
        ResponseEntity<String> unauthSendResp = restTemplate.exchange(
                "/api/messages",
                HttpMethod.POST,
                new HttpEntity<>(unauthSend, user3Headers),
                String.class
        );
        assertThat(unauthSendResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> unauthGetResp = restTemplate.exchange(
                "/api/conversations/" + conversationPublicId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(user3Headers),
                String.class
        );
        assertThat(unauthGetResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // =================================================================
        // 5. Unread Counts across multiple messages
        // =================================================================
        SendMessageRequest sendReq2 = new SendMessageRequest(
                conversationPublicId,
                UUID.randomUUID().toString(),
                "Second message",
                MessageType.TEXT
        );
        restTemplate.exchange("/api/messages", HttpMethod.POST, new HttpEntity<>(sendReq2, user1Headers), String.class);

        SendMessageRequest sendReq3 = new SendMessageRequest(
                conversationPublicId,
                UUID.randomUUID().toString(),
                "Third message",
                MessageType.TEXT
        );
        restTemplate.exchange("/api/messages", HttpMethod.POST, new HttpEntity<>(sendReq3, user1Headers), String.class);

        convsUser2 = restTemplate.exchange(
                "/api/conversations",
                HttpMethod.GET,
                new HttpEntity<>(user2Headers),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ConversationSummaryResponse>>>() {}
        );
        user2Summary = convsUser2.getBody().data().content().stream()
                .filter(c -> c.publicId().equals(conversationPublicId))
                .findFirst()
                .orElseThrow();
        assertThat(user2Summary.unreadCount()).isEqualTo(3);
        assertThat(user2Summary.lastMessagePreview()).isEqualTo("Third message");

        // =================================================================
        // 6. Pagination across messages with IDENTICAL timestamps
        // =================================================================
        // Insert 5 messages with exact same timestamp directly into this conversation
        Conversation conv = conversationRepository.findByPublicId(conversationPublicId).orElseThrow();
        User senderUser = userRepository.findByUsername(user1Username).orElseThrow();
        Instant fixedTimestamp = Instant.parse("2026-11-26T10:00:00Z");

        for (int i = 1; i <= 5; i++) {
            Message batchMsg = new Message();
            batchMsg.setClientMessageId("batch-identical-" + UUID.randomUUID());
            batchMsg.setConversation(conv);
            batchMsg.setSender(senderUser);
            batchMsg.setContent("Batch message " + i);
            batchMsg.setMessageType(MessageType.TEXT);
            batchMsg.setCreatedAt(fixedTimestamp);
            messageRepository.save(batchMsg);
        }

        // Now test keyset pagination with size = 2
        // Page 1: newest messages
        ResponseEntity<CursorApiResponse<MessageResponse>> page1Resp = restTemplate.exchange(
                "/api/conversations/" + conversationPublicId + "/messages?size=2",
                HttpMethod.GET,
                new HttpEntity<>(user1Headers),
                new ParameterizedTypeReference<CursorApiResponse<MessageResponse>>() {}
        );
        assertThat(page1Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CursorApiResponse<MessageResponse> page1 = page1Resp.getBody();
        assertThat(page1).isNotNull();
        assertThat(page1.data()).hasSize(2);
        assertThat(page1.hasMore()).isTrue();
        assertThat(page1.nextCursor()).isNotNull();

        // Page 2: with before cursor from Page 1
        ResponseEntity<CursorApiResponse<MessageResponse>> page2Resp = restTemplate.exchange(
                "/api/conversations/" + conversationPublicId + "/messages?size=2&before=" + page1.nextCursor(),
                HttpMethod.GET,
                new HttpEntity<>(user1Headers),
                new ParameterizedTypeReference<CursorApiResponse<MessageResponse>>() {}
        );
        assertThat(page2Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CursorApiResponse<MessageResponse> page2 = page2Resp.getBody();
        assertThat(page2.data()).hasSize(2);
        assertThat(page2.hasMore()).isTrue();
        assertThat(page2.nextCursor()).isNotNull();

        // Check no overlap between page 1 and page 2
        List<String> page1Ids = page1.data().stream().map(MessageResponse::publicId).toList();
        List<String> page2Ids = page2.data().stream().map(MessageResponse::publicId).toList();
        assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    }
}
