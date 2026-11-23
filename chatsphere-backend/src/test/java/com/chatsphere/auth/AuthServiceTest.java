package com.chatsphere.auth;

import com.chatsphere.auth.dto.LoginRequest;
import com.chatsphere.auth.dto.RegisterRequest;
import com.chatsphere.auth.entity.RefreshToken;
import com.chatsphere.auth.repository.EmailVerificationTokenRepository;
import com.chatsphere.auth.repository.RefreshTokenRepository;
import com.chatsphere.auth.service.AuthService;
import com.chatsphere.auth.service.EmailService;
import com.chatsphere.common.enums.AccountStatus;
import com.chatsphere.exception.ConflictException;
import com.chatsphere.exception.UnauthorizedException;
import com.chatsphere.security.JwtService;
import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordEncoder,
                jwtService,
                emailService,
                604800000L
        );
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("Alice Johnson", "alice_j", "alice@example.com", "SecurePass123!");

        given(userRepository.existsByUsername("alice_j")).willReturn(false);
        given(userRepository.existsByEmail("alice@example.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("$2a$12$hashedPassword");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            u.setPublicId(UUID.randomUUID().toString());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("alice_j");
        assertThat(response.email()).isEqualTo("alice@example.com");
        verify(emailService, times(1)).sendVerificationEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void register_DuplicateUsername_ThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Alice Johnson", "alice_j", "alice@example.com", "SecurePass123!");
        given(userRepository.existsByUsername("alice_j")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Alice Johnson", "alice_j", "alice@example.com", "SecurePass123!");
        given(userRepository.existsByUsername("alice_j")).willReturn(false);
        given(userRepository.existsByEmail("alice@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("alice_j", "SecurePass123!");
        User user = new User();
        user.setId(1L);
        user.setUsername("alice_j");
        user.setEmail("alice@example.com");
        user.setPasswordHash("$2a$12$hashedPassword");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPublicId(UUID.randomUUID().toString());

        given(userRepository.findByUsernameOrEmail("alice_j", "alice_j")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("SecurePass123!", "$2a$12$hashedPassword")).willReturn(true);
        given(jwtService.generateToken(anyString(), anyString())).willReturn("mock.jwt.token");
        given(jwtService.getExpirationMs()).willReturn(900000L);

        AuthService.AuthResult result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.authResponse().accessToken()).isEqualTo("mock.jwt.token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void login_WrongPassword_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("alice_j", "WrongPassword!");
        User user = new User();
        user.setPasswordHash("$2a$12$hashedPassword");

        given(userRepository.findByUsernameOrEmail("alice_j", "alice_j")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("WrongPassword!", "$2a$12$hashedPassword")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username/email or password");

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void login_NonExistentUser_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("unknown_user", "Password123!");
        given(userRepository.findByUsernameOrEmail("unknown_user", "unknown_user")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username/email or password");
    }

    @Test
    void refreshToken_Success_RotatesToken() {
        String rawToken = "sample-raw-refresh-token";
        User user = new User();
        user.setId(10L);
        user.setUsername("bob");
        user.setPublicId(UUID.randomUUID().toString());

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(1L);
        storedToken.setUser(user);
        storedToken.setRevoked(false);
        storedToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(storedToken));
        given(jwtService.generateToken(anyString(), anyString())).willReturn("new.jwt.token");
        given(jwtService.getExpirationMs()).willReturn(900000L);

        AuthService.AuthResult result = authService.refreshToken(rawToken);

        assertThat(result).isNotNull();
        assertThat(result.authResponse().accessToken()).isEqualTo("new.jwt.token");
        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(storedToken); // Old token marked revoked
        verify(refreshTokenRepository, times(1)).save(argThat(t -> !t.equals(storedToken))); // New token saved
    }

    @Test
    void refreshToken_RevokedToken_ThrowsUnauthorizedAndRevokesAll() {
        String rawToken = "compromised-token";
        User user = new User();
        user.setId(10L);

        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setUser(user);
        revokedToken.setRevoked(true);

        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refreshToken(rawToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Revoked refresh token presented");

        verify(refreshTokenRepository, times(1)).revokeAllByUserId(10L);
    }

    @Test
    void logout_RevokesToken() {
        String rawToken = "active-token";
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);

        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token));

        authService.logout(rawToken);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(token);
    }
}
