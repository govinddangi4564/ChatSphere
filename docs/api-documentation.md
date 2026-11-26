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


---

## 5. Conversations APIs (`/api/conversations`)

### Create or Find Private Conversation
- **Endpoint:** `POST /api/conversations/private`
- **Access:** Authenticated
- **Rules:**
  - Idempotent: returns existing 1-on-1 conversation if one already exists.
  - Enforces database-level duplicate prevention via unique `private_pair_hash`.
  - Rejects self-conversations (`400 BAD_REQUEST`).
  - Enforces bidirectional block rules (`404 NOT_FOUND`).
- **Request Body (`CreatePrivateConversationRequest`):**
```json
{
  "recipientPublicId": "550e8400-e29b-41d4-a716-446655440001"
}
```
- **Response (`201 CREATED`):**
```json
{
  "success": true,
  "message": "Conversation retrieved successfully",
  "data": {
    "publicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
    "type": "PRIVATE",
    "title": "Bob Jones",
    "imageUrl": "https://example.com/bob.jpg",
    "createdAt": "2026-11-24T20:00:00Z",
    "lastMessageAt": null,
    "participants": [
      {
        "publicId": "550e8400-e29b-41d4-a716-446655440000",
        "fullName": "Alice Smith",
        "username": "alice",
        "profileImageUrl": null,
        "lastSeenAt": "2026-11-24T21:40:00Z",
        "role": "MEMBER"
      },
      {
        "publicId": "550e8400-e29b-41d4-a716-446655440001",
        "fullName": "Bob Jones",
        "username": "bob",
        "profileImageUrl": "https://example.com/bob.jpg",
        "lastSeenAt": "2026-11-24T21:35:00Z",
        "role": "MEMBER"
      }
    ],
    "muted": false,
    "archived": false,
    "unreadCount": 0,
    "role": "MEMBER"
  }
}
```

### List Conversations
- **Endpoint:** `GET /api/conversations?page=0&size=20&sort=lastMessageAt,desc&type=PRIVATE&archived=false&muted=false&unread=false`
- **Access:** Authenticated
- **Rules:**
  - Whitelisted sort fields: `lastMessageAt`, `createdAt`, `title`.
  - Pagination limits: max `size` = 50.
  - Optional filters: `type` (`PRIVATE`, `GROUP`), `archived` (`true`/`false`), `muted` (`true`/`false`), `unread` (`true`/`false`).
  - Batch-loads participants in a single query to eliminate N+1 overhead.
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": {
    "content": [
      {
        "publicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
        "type": "PRIVATE",
        "title": "Bob Jones",
        "imageUrl": "https://example.com/bob.jpg",
        "lastMessageAt": null,
        "lastMessagePreview": null,
        "unreadCount": 0,
        "muted": false,
        "archived": false,
        "participant": {
          "publicId": "550e8400-e29b-41d4-a716-446655440001",
          "fullName": "Bob Jones",
          "username": "bob",
          "profileImageUrl": "https://example.com/bob.jpg",
          "lastSeenAt": "2026-11-24T21:35:00Z",
          "role": "MEMBER"
        }
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

### Get Conversation Details
- **Endpoint:** `GET /api/conversations/{publicId}`
- **Access:** Authenticated (caller must be an active member, returns 404/403 otherwise).
- **Response (`200 OK`):** Full conversation details and participant list.

### Archive Conversation
- **Endpoint:** `PUT /api/conversations/{publicId}/archive`
- **Access:** Authenticated
- **Request Body (`ArchiveRequest`):** `{ "archived": true }`
- **Response (`200 OK`):** `{ "success": true, "message": "Conversation archived", "data": null }`

### Mute Conversation
- **Endpoint:** `PUT /api/conversations/{publicId}/mute`
- **Access:** Authenticated
- **Request Body (`MuteRequest`):** `{ "muted": true }`
- **Response (`200 OK`):** `{ "success": true, "message": "Conversation muted", "data": null }`

---

## 6. Messages APIs (`/api/messages`)

### Send Message
- **Endpoint:** `POST /api/messages`
- **Access:** Authenticated (caller must be an active member of the conversation)
- **Features:**
  - Idempotency enforced via `(sender_id, client_message_id)`. Re-submitting the same clientMessageId returns the existing message without duplicate row creation or duplicate unread increment.
  - Server-side timestamp assignment.
  - Automatically generates delivery receipt rows for all conversation recipients with `deliveredAt=null` and `readAt=null`.
  - Atomically increments unread counts for all other members.
  - Updates conversation `lastMessageAt` and `lastMessagePreview` within the same transaction.
- **Request Body (`SendMessageRequest`):**
```json
{
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "clientMessageId": "d03e2239-1667-4228-bc88-9bb31f795db7",
  "content": "Hello Bob! How are you doing?",
  "messageType": "TEXT"
}
```
- **Response (`201 CREATED`):**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "publicId": "e1f10c66-4e55-472e-8e67-d8c9735d10a2",
    "clientMessageId": "d03e2239-1667-4228-bc88-9bb31f795db7",
    "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
    "senderPublicId": "550e8400-e29b-41d4-a716-446655440000",
    "senderUsername": "alice_j",
    "senderFullName": "Alice Johnson",
    "messageType": "TEXT",
    "content": "Hello Bob! How are you doing?",
    "createdAt": "2026-11-26T18:25:00Z",
    "updatedAt": null,
    "edited": false,
    "deletedForEveryone": false
  }
}
```

### Get Conversation Messages (Keyset Cursor Pagination)
- **Endpoint:** `GET /api/conversations/{publicId}/messages?size=30&before=cursor`
- **Access:** Authenticated (caller must be an active member of the conversation)
- **Features:**
  - Keyset cursor pagination using `(created_at, id)` composite index for stable tie-breaking.
  - Default size: 30, maximum: 100.
  - Returns messages in chronological display order directly from SQL without in-memory sorting.
  - Returns opaque URL-safe cursor `nextCursor` and boolean `hasMore`.
- **Response (`200 OK`):**
```json
{
  "success": true,
  "message": "Messages retrieved successfully",
  "data": [
    {
      "publicId": "e1f10c66-4e55-472e-8e67-d8c9735d10a2",
      "clientMessageId": "d03e2239-1667-4228-bc88-9bb31f795db7",
      "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
      "senderPublicId": "550e8400-e29b-41d4-a716-446655440000",
      "senderUsername": "alice_j",
      "senderFullName": "Alice Johnson",
      "messageType": "TEXT",
      "content": "Hello Bob! How are you doing?",
      "createdAt": "2026-11-26T18:25:00Z",
      "updatedAt": null,
      "edited": false,
      "deletedForEveryone": false
    }
  ],
  "nextCursor": "MjAyNi0xMS0yNlQxODoyNTowMFox",
  "hasMore": false
}
```
