package com.chatsphere.websocket.controller;

import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.SendMessageRequest;
import com.chatsphere.message.service.MessageService;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import com.chatsphere.websocket.dto.DeliveryReceiptRequest;
import com.chatsphere.websocket.dto.ReadReceiptRequest;
import com.chatsphere.websocket.dto.ReceiptNotification;
import com.chatsphere.websocket.dto.TypingEventRequest;
import com.chatsphere.websocket.dto.TypingEventResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public ChatWebSocketController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository
    ) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequest request, Principal principal) {
        log.debug("STOMP /chat.send from principal: {}", principal.getName());
        // Server derives sender from authenticated principal, never from payload
        MessageResponse response = messageService.sendMessage(principal.getName(), request);

        // Broadcast to conversation destination for all active members
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + request.conversationPublicId(),
                response
        );
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Valid @Payload TypingEventRequest request, Principal principal) {
        log.debug("STOMP /chat.typing from principal: {}", principal.getName());
        User user = getUserByUsername(principal.getName());

        TypingEventResponse response = new TypingEventResponse(
                request.conversationPublicId(),
                user.getPublicId(),
                user.getUsername(),
                request.typing()
        );

        // Ephemeral: broadcast to conversation without database persistence
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + request.conversationPublicId() + "/typing",
                response
        );
    }

    @MessageMapping("/chat.delivered")
    public void handleDelivered(@Valid @Payload DeliveryReceiptRequest request, Principal principal) {
        log.debug("STOMP /chat.delivered from principal: {}", principal.getName());
        ReceiptNotification notification = messageService.markMessageDelivered(principal.getName(), request.messagePublicId());
        String senderUsername = messageService.getMessageSenderUsername(request.messagePublicId());

        // Notify the original message sender on their private queue
        messagingTemplate.convertAndSendToUser(
                senderUsername,
                "/queue/receipts",
                notification
        );
    }

    @MessageMapping("/chat.read")
    public void handleRead(@Valid @Payload ReadReceiptRequest request, Principal principal) {
        log.debug("STOMP /chat.read from principal: {}", principal.getName());
        List<ReceiptNotification> notifications = messageService.markConversationRead(principal.getName(), request.conversationPublicId());

        // Notify senders on their private queue
        for (ReceiptNotification notification : notifications) {
            String senderUsername = messageService.getMessageSenderUsername(notification.messagePublicId());
            messagingTemplate.convertAndSendToUser(
                    senderUsername,
                    "/queue/receipts",
                    notification
            );
        }
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
