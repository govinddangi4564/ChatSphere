package com.chatsphere.auth.controller;

import com.chatsphere.auth.dto.*;
import com.chatsphere.auth.service.AuthService;
import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, authentication, token refresh and password recovery endpoints")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "chatsphere_refresh_token";

    private final AuthService authService;
    private final long refreshExpirationMs;

    public AuthController(
            AuthService authService,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.authService = authService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a new user account and dispatches an email verification token")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Please verify your email.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials, sets HttpOnly refresh cookie, and returns JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.login(request);
        setRefreshTokenCookie(response, result.rawRefreshToken(), refreshExpirationMs / 1000);
        return ResponseEntity.ok(ApiResponse.success("Login successful", result.authResponse()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token via HttpOnly cookie and returns new JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, result.rawRefreshToken(), refreshExpirationMs / 1000);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", result.authResponse()));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes refresh token and clears HttpOnly refresh cookie")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Validates the verification token sent via email and activates the user account")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiates password reset workflow and dispatches reset link")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If an account exists with this email, a reset link has been dispatched.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Sets new password using valid reset token and terminates existing sessions")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You may now log in with your new credentials.", null));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // Can be toggled true in production with HTTPS
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
