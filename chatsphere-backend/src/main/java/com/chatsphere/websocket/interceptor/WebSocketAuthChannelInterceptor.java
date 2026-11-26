package com.chatsphere.websocket.interceptor;

import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.security.CustomUserDetailsService;
import com.chatsphere.security.JwtService;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    public WebSocketAuthChannelInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository,
            ConversationMemberRepository conversationMemberRepository
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getFirstNativeHeader("token");
        }
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getPasscode();
        }

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("STOMP CONNECT rejected: missing Authorization header");
            throw new AuthenticationCredentialsNotFoundException("Missing Authorization header in STOMP CONNECT frame");
        }

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        if (!jwtService.validateToken(token)) {
            log.warn("STOMP CONNECT rejected: invalid or expired JWT token");
            throw new BadCredentialsException("Invalid or expired JWT token in STOMP CONNECT frame");
        }

        String username = jwtService.extractUsername(token);
        if (username == null || username.isBlank()) {
            throw new BadCredentialsException("Token does not contain a valid username subject");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        accessor.setUser(authentication);
        log.debug("STOMP CONNECT authenticated successfully for user: {}", username);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("STOMP SUBSCRIBE rejected: unauthenticated session");
            throw new AccessDeniedException("Unauthenticated subscription attempt");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        // Rule 1: Conversation destination authorization: /topic/conversations/{conversationPublicId}
        if (destination.startsWith("/topic/conversations/")) {
            String conversationPublicId = destination.substring("/topic/conversations/".length());
            // Strip any trailing slashes or subpaths
            int slashIdx = conversationPublicId.indexOf('/');
            if (slashIdx > 0) {
                conversationPublicId = conversationPublicId.substring(0, slashIdx);
            }

            Optional<User> userOpt = userRepository.findByUsername(principal.getName());
            if (userOpt.isEmpty()) {
                throw new AccessDeniedException("User not found");
            }

            boolean isMember = conversationMemberRepository
                    .findByConversationPublicIdAndUserId(conversationPublicId, userOpt.get().getId())
                    .isPresent();

            if (!isMember) {
                log.warn("STOMP SUBSCRIBE forbidden: user {} is not a member of conversation {}", principal.getName(), conversationPublicId);
                throw new AccessDeniedException("Forbidden: you are not a member of conversation " + conversationPublicId);
            }
        }
    }
}
