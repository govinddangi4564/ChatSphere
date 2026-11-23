package com.chatsphere.auth.service;

import com.chatsphere.auth.dto.*;
import com.chatsphere.auth.entity.EmailVerificationToken;
import com.chatsphere.auth.entity.RefreshToken;
import com.chatsphere.auth.repository.EmailVerificationTokenRepository;
import com.chatsphere.auth.repository.RefreshTokenRepository;
import com.chatsphere.common.enums.AccountStatus;
import com.chatsphere.common.enums.TokenType;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ConflictException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.exception.TokenExpiredException;
import com.chatsphere.exception.UnauthorizedException;
import com.chatsphere.security.JwtService;
import com.chatsphere.user.dto.UserResponse;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.mapper.UserMapper;
import com.chatsphere.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final long refreshExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setPublicId(UUID.randomUUID().toString());
        user.setFullName(request.fullName().trim());
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Generate email verification token
        String rawToken = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(savedUser);
        verificationToken.setTokenHash(hashToken(rawToken));
        verificationToken.setTokenType(TokenType.EMAIL_VERIFICATION);
        verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), rawToken);

        return UserMapper.toUserResponse(savedUser);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username/email or password");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Account is " + user.getAccountStatus().name().toLowerCase());
        }

        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getUsername(), user.getPublicId());
        String rawRefreshToken = generateAndSaveRefreshToken(user);

        UserResponse userResponse = UserMapper.toUserResponse(user);
        AuthResponse authResponse = AuthResponse.of(accessToken, jwtService.getExpirationMs() / 1000, userResponse);

        return new AuthResult(authResponse, rawRefreshToken);
    }

    @Transactional
    public AuthResult refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // Potential compromise: revoke all active tokens for this user
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId());
            throw new UnauthorizedException("Revoked refresh token presented. All active sessions have been terminated.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenExpiredException("Refresh token has expired");
        }

        // Token rotation: revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user.getUsername(), user.getPublicId());
        String newRawRefreshToken = generateAndSaveRefreshToken(user);

        UserResponse userResponse = UserMapper.toUserResponse(user);
        AuthResponse authResponse = AuthResponse.of(newAccessToken, jwtService.getExpirationMs() / 1000, userResponse);

        return new AuthResult(authResponse, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = hashToken(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHashAndTokenType(tokenHash, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (token.getVerifiedAt() != null) {
            throw new BadRequestException("Email is already verified");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Verification token has expired");
        }

        token.setVerifiedAt(Instant.now());
        emailVerificationTokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            EmailVerificationToken token = new EmailVerificationToken();
            token.setUser(user);
            token.setTokenHash(hashToken(rawToken));
            token.setTokenType(TokenType.PASSWORD_RESET);
            token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            emailVerificationTokenRepository.save(token);

            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token());
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHashAndTokenType(tokenHash, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (token.getVerifiedAt() != null) {
            throw new BadRequestException("Password reset token has already been used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        token.setVerifiedAt(Instant.now());
        emailVerificationTokenRepository.save(token);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Invalidate all active refresh tokens after password change
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    private String generateAndSaveRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(Instant.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record AuthResult(AuthResponse authResponse, String rawRefreshToken) {}
}
