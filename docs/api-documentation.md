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


---

## 3. User Management APIs (`/api/users`)

### Get Current User Profile
- **Endpoint:** `GET /api/users/me`
- **Access:** Authenticated
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "publicId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Alice Johnson",
    "username": "alice_j",
    "email": "alice@example.com",
    "profileImageUrl": "https://example.com/avatar.jpg",
    "bio": "Building with ChatSphere",
    "accountStatus": "ACTIVE",
    "emailVerified": true,
    "createdAt": "2026-11-24T18:00:00Z"
  }
}
```

### Update Current User Profile
- **Endpoint:** `PUT /api/users/me`
- **Access:** Authenticated
- **Request Body (`UpdateProfileRequest`):**
```json
{
  "fullName": "Alice J. Smith",
  "bio": "Software Engineer & Designer",
  "profileImageUrl": "https://example.com/new-avatar.jpg"
}
```
*Note: Accepts image URL in Phase 3. Direct upload to cloud storage arrives in Phase 7.*
- **Response (`200 OK`):** Updated user profile payload.

### Search Users
- **Endpoint:** `GET /api/users/search?query=alice&page=0&size=20&sort=username,asc`
- **Access:** Authenticated
- **Rules:**
  - Case-insensitive search on username and full name.
  - Wildcard characters (`%`, `_`, `\`) safely escaped.
  - Sort whitelist: `username`, `fullName`.
  - Excludes requesting user and any users involved in bidirectional blocks.
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": {
    "content": [
      {
        "publicId": "79b4d898-9db8-4061-949d-648b78997ef2",
        "fullName": "Alice Walker",
        "username": "alicew",
        "profileImageUrl": "https://example.com/pic.jpg",
        "bio": "Hello there!",
        "lastSeenAt": "2026-11-24T20:15:00Z",
        "isContact": false,
        "isBlocked": false
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

### Get Public Profile
- **Endpoint:** `GET /api/users/{publicId}`
- **Access:** Authenticated
- **Rules:** Respects target user's privacy visibility settings (`profilePhotoVisibility` and `lastSeenVisibility`). If blocked by target, returns `404 NOT_FOUND`.

### Block User
- **Endpoint:** `POST /api/users/{publicId}/block`
- **Access:** Authenticated
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "User blocked successfully",
  "data": null
}
```

### Unblock User
- **Endpoint:** `DELETE /api/users/{publicId}/block`
- **Access:** Authenticated
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "User unblocked successfully",
  "data": null
}
```

### Get Blocked Users
- **Endpoint:** `GET /api/users/blocked`
- **Access:** Authenticated
- **Response (`200 OK`):** List of blocked user records (`publicId`, `fullName`, `username`, `profileImageUrl`, `blockedAt`).

### Get Privacy Settings
- **Endpoint:** `GET /api/users/privacy`
- **Access:** Authenticated
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Privacy settings retrieved successfully",
  "data": {
    "profilePhotoVisibility": "EVERYONE",
    "lastSeenVisibility": "CONTACTS_ONLY",
    "onlineStatusVisibility": "EVERYONE",
    "messagePermission": "EVERYONE"
  }
}
```

### Update Privacy Settings
- **Endpoint:** `PUT /api/users/privacy`
- **Access:** Authenticated
- **Request Body (`PrivacySettingsRequest`):**
```json
{
  "profilePhotoVisibility": "CONTACTS_ONLY",
  "lastSeenVisibility": "NOBODY",
  "onlineStatusVisibility": "EVERYONE",
  "messagePermission": "CONTACTS_ONLY"
}
```

### Update Last Seen Heartbeat
- **Endpoint:** `POST /api/users/presence/heartbeat`
- **Access:** Authenticated
- **Response (`200 OK`):** `{ "success": true, "message": "Presence updated", "data": null }`

---

## 4. Contacts APIs (`/api/contacts`)

### List Contacts
- **Endpoint:** `GET /api/contacts`
- **Access:** Authenticated
- **Response (`200 OK`):** List of contacts with privacy visibility checks applied.

### Add Contact
- **Endpoint:** `POST /api/contacts/{publicId}`
- **Access:** Authenticated
- **Rules:** Cannot add oneself. Throws 409 if already in contacts. Throws 404 if blocked by target.
- **Response (`201 CREATED`):** Contact record details.

### Remove Contact
- **Endpoint:** `DELETE /api/contacts/{publicId}`
- **Access:** Authenticated
- **Response (`200 OK`):** `{ "success": true, "message": "Contact removed successfully", "data": null }`
