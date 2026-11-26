package com.chatsphere.websocket;

import com.chatsphere.auth.dto.AuthResponse;
import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.dto.ConversationDetailsResponse;
import com.chatsphere.conversation.dto.CreatePrivateConversationRequest;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.SendMessageRequest;
import com.chatsphere.message.entity.Message;
import com.chatsphere.message.entity.MessageReceipt;
import com.chatsphere.message.repository.MessageReceiptRepository;
import com.chatsphere.message.repository.MessageRepository;
import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import com.chatsphere.websocket.dto.DeliveryReceiptRequest;
import com.chatsphere.websocket.dto.ReadReceiptRequest;
import com.chatsphere.websocket.dto.ReceiptNotification;
import com.chatsphere.websocket.dto.TypingEventRequest;
import com.chatsphere.websocket.dto.TypingEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReceiptRepository messageReceiptRepository;

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

        user1Username = "ws1_" + suffix;
        user1PublicId = registerUser("WS User One", user1Username, user1Username + "@example.com");
        user1Token = loginUser(user1Username);

        user2Username = "ws2_" + suffix;
        user2PublicId = registerUser("WS User Two", user2Username, user2Username + "@example.com");
        user2Token = loginUser(user2Username);

        user3Username = "ws3_" + suffix;
        user3PublicId = registerUser("WS User Three", user3Username, user3Username + "@example.com");
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

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);
        return client;
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
    void testConnection_WithoutToken_Fails() {
        WebSocketStompClient client = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";

        StompHeaders headers = new StompHeaders();
        // No Authorization header

        CompletableFuture<StompSession> sessionFuture = client.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {}
        );

        assertThatThrownBy(() -> sessionFuture.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testConnection_WithValidToken_Succeeds() throws Exception {
        WebSocketStompClient client = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";

        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + user1Token);

        StompSession session = client.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    void testSubscription_Unauthorized_Fails() throws Exception {
        WebSocketStompClient client = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";

        // User 3 connects (not a member of the conversation between User 1 and User 2)
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + user3Token);

        CompletableFuture<Throwable> errorFuture = new CompletableFuture<>();

        StompSession session = client.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession s, StompCommand command, StompHeaders h, byte[] payload, Throwable exception) {
                        errorFuture.complete(exception);
                    }

                    @Override
                    public void handleTransportError(StompSession s, Throwable exception) {
                        errorFuture.complete(exception);
                    }
                }
        ).get(5, TimeUnit.SECONDS);

        // User 3 attempts to subscribe to the private conversation
        session.subscribe("/topic/conversations/" + conversationPublicId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {}
        });

        // The subscription will be rejected by the channel interceptor and session will receive an error or disconnect
        Throwable error = errorFuture.get(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();

        if (session.isConnected()) {
            session.disconnect();
        }
    }

    @Test
    void testRealTimeMessageDelivery_Typing_And_Receipts() throws Exception {
        WebSocketStompClient client1 = createStompClient();
        WebSocketStompClient client2 = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";

        // Connect User 1 (Sender)
        StompHeaders headers1 = new StompHeaders();
        headers1.add("Authorization", "Bearer " + user1Token);
        StompSession session1 = client1.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                headers1,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // Connect User 2 (Recipient)
        StompHeaders headers2 = new StompHeaders();
        headers2.add("Authorization", "Bearer " + user2Token);
        StompSession session2 = client2.connectAsync(
                wsUrl,
                new WebSocketHttpHeaders(),
                headers2,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // =================================================================
        // 1. User 2 subscribes to conversation topic
        // =================================================================
        CompletableFuture<MessageResponse> receivedMessageFuture = new CompletableFuture<>();
        CompletableFuture<TypingEventResponse> receivedTypingFuture = new CompletableFuture<>();

        session2.subscribe("/topic/conversations/" + conversationPublicId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessageFuture.complete((MessageResponse) payload);
            }
        });

        session2.subscribe("/topic/conversations/" + conversationPublicId + "/typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TypingEventResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedTypingFuture.complete((TypingEventResponse) payload);
            }
        });

        // User 1 subscribes to their private receipt queue
        CompletableFuture<ReceiptNotification> receivedReceiptFuture = new CompletableFuture<>();
        session1.subscribe("/user/queue/receipts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ReceiptNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedReceiptFuture.complete((ReceiptNotification) payload);
            }
        });

        // =================================================================
        // 2. Typing Event: User 1 types
        // =================================================================
        TypingEventRequest typingReq = new TypingEventRequest(conversationPublicId, true);
        session1.send("/app/chat.typing", typingReq);

        TypingEventResponse typingResp = receivedTypingFuture.get(5, TimeUnit.SECONDS);
        assertThat(typingResp).isNotNull();
        assertThat(typingResp.conversationPublicId()).isEqualTo(conversationPublicId);
        assertThat(typingResp.senderUsername()).isEqualTo(user1Username);
        assertThat(typingResp.typing()).isTrue();

        // =================================================================
        // 3. Send Message: User 1 sends message via STOMP /app/chat.send
        // =================================================================
        String clientMsgId = UUID.randomUUID().toString();
        SendMessageRequest sendReq = new SendMessageRequest(
                conversationPublicId,
                clientMsgId,
                "Hello via WebSocket!",
                MessageType.TEXT
        );
        session1.send("/app/chat.send", sendReq);

        // User 2 receives the message in real time
        MessageResponse receivedMsg = receivedMessageFuture.get(5, TimeUnit.SECONDS);
        assertThat(receivedMsg).isNotNull();
        assertThat(receivedMsg.content()).isEqualTo("Hello via WebSocket!");
        assertThat(receivedMsg.clientMessageId()).isEqualTo(clientMsgId);
        assertThat(receivedMsg.senderUsername()).isEqualTo(user1Username);

        // Verify message was persisted to MySQL
        Message persistedMsg = messageRepository.findByPublicId(receivedMsg.publicId()).orElseThrow();
        assertThat(persistedMsg.getContent()).isEqualTo("Hello via WebSocket!");

        // =================================================================
        // 4. Delivered Receipt: User 2 acknowledges delivery
        // =================================================================
        DeliveryReceiptRequest deliveredReq = new DeliveryReceiptRequest(receivedMsg.publicId());
        session2.send("/app/chat.delivered", deliveredReq);

        // User 1 receives delivered receipt notification
        ReceiptNotification deliveredNotification = receivedReceiptFuture.get(5, TimeUnit.SECONDS);
        assertThat(deliveredNotification).isNotNull();
        assertThat(deliveredNotification.messagePublicId()).isEqualTo(receivedMsg.publicId());
        assertThat(deliveredNotification.status()).isEqualTo("DELIVERED");

        // Verify receipt row in DB has delivered_at set
        User user2 = userRepository.findByUsername(user2Username).orElseThrow();
        MessageReceipt receipt = messageReceiptRepository.findByMessagePublicIdAndRecipientId(receivedMsg.publicId(), user2.getId()).orElseThrow();
        assertThat(receipt.getDeliveredAt()).isNotNull();

        // =================================================================
        // 5. Read Receipt: User 2 marks conversation as read
        // =================================================================
        CompletableFuture<ReceiptNotification> readReceiptFuture = new CompletableFuture<>();
        session1.subscribe("/user/queue/receipts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ReceiptNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if ("READ".equals(((ReceiptNotification) payload).status())) {
                    readReceiptFuture.complete((ReceiptNotification) payload);
                }
            }
        });

        ReadReceiptRequest readReq = new ReadReceiptRequest(conversationPublicId);
        session2.send("/app/chat.read", readReq);

        ReceiptNotification readNotification = readReceiptFuture.get(5, TimeUnit.SECONDS);
        assertThat(readNotification).isNotNull();
        assertThat(readNotification.status()).isEqualTo("READ");

        // Verify DB receipt has read_at set
        MessageReceipt updatedReceipt = messageReceiptRepository.findByMessagePublicIdAndRecipientId(receivedMsg.publicId(), user2.getId()).orElseThrow();
        assertThat(updatedReceipt.getReadAt()).isNotNull();

        session1.disconnect();
        session2.disconnect();
    }
}
