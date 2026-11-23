# ChatSphere API Documentation

## 1. Standard Response Formats

### Success Response (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

### Error Response (`ErrorResponse`)
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email address"
    }
  ]
}
```

---

## 2. Authentication APIs (`/api/auth`)

### Register User
- **Endpoint:** `POST /api/auth/register`
- **Access:** Public
- **Request Body (`RegisterRequest`):**
```json
{
  "fullName": "Alice Johnson",
  "username": "alice_j",
  "email": "alice@example.com",
  "password": "Password123!"
}
```
- **Response (`201 CREATED`):**
```json
{
  "success": true,
  "message": "User registered successfully. Please verify your email.",
  "data": {
    "publicId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Alice Johnson",
    "username": "alice_j",
    "email": "alice@example.com",
    "profileImageUrl": null,
    "bio": null,
    "accountStatus": "ACTIVE",
    "emailVerified": false,
    "createdAt": "2026-11-23T18:00:00Z"
  }
}
```

### User Login
- **Endpoint:** `POST /api/auth/login`
- **Access:** Public
- **Request Body (`LoginRequest`):**
```json
{
  "usernameOrEmail": "alice_j",
  "password": "Password123!"
}
```
- **Response (`200 OK`):**
  - Sets `Set-Cookie: chatsphere_refresh_token=<token>; Path=/api/auth; HttpOnly; SameSite=Lax`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "publicId": "550e8400-e29b-41d4-a716-446655440000",
      "fullName": "Alice Johnson",
      "username": "alice_j",
      "email": "alice@example.com",
      "profileImageUrl": null,
      "bio": null,
      "accountStatus": "ACTIVE",
      "emailVerified": false,
      "createdAt": "2026-11-23T18:00:00Z"
    }
  }
}
```

### Refresh Token (Token Rotation)
- **Endpoint:** `POST /api/auth/refresh`
- **Access:** Public (Requires `chatsphere_refresh_token` HttpOnly cookie)
- **Response (`200 OK`):**
  - Revokes old refresh token and sets rotated `chatsphere_refresh_token` in cookie.
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { ... }
  }
}
```

### User Logout
- **Endpoint:** `POST /api/auth/logout`
- **Access:** Public (Requires `chatsphere_refresh_token` HttpOnly cookie)
- **Response (`200 OK`):**
  - Clears `chatsphere_refresh_token` cookie and revokes token in database.
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

### Verify Email
- **Endpoint:** `POST /api/auth/verify-email`
- **Access:** Public
- **Request Body (`VerifyEmailRequest`):**
```json
{
  "token": "e3130f58-86dd-4b4d-b992-10a9512a3383"
}
```
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": null
}
```

### Forgot Password
- **Endpoint:** `POST /api/auth/forgot-password`
- **Access:** Public
- **Request Body (`ForgotPasswordRequest`):**
```json
{
  "email": "alice@example.com"
}
```
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "If an account exists with this email, a reset link has been dispatched.",
  "data": null
}
```

### Reset Password
- **Endpoint:** `POST /api/auth/reset-password`
- **Access:** Public
- **Request Body (`ResetPasswordRequest`):**
```json
{
  "token": "e3130f58-86dd-4b4d-b992-10a9512a3383",
  "newPassword": "NewPassword123!"
}
```
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Password reset successfully. You may now log in with your new credentials.",
  "data": null
}
```

---

## 3. System & Health Endpoints

### Health Check
- **Endpoint:** `GET /api/health`
- **Access:** Public
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "ChatSphere API is operational",
  "data": {
    "status": "UP",
    "timestamp": "2026-11-23T18:50:00Z",
    "database": "UP",
    "version": "1.0.0"
  }
}
```

### Swagger Documentation
- **UI:** `GET /swagger-ui.html`
- **OpenAPI JSON:** `GET /v3/api-docs`
