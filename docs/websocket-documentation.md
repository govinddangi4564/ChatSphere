# ChatSphere WebSocket and STOMP Protocol Documentation

ChatSphere provides a real-time bi-directional messaging layer built on **Spring WebSocket**, **STOMP (Simple Text Oriented Messaging Protocol)**, and **SockJS** fallback support.

---

## 1. Handshake & Connection

### Connection URLs
- **Native WebSocket**: `ws://localhost:8080/ws` (or `wss://` in production)
- **SockJS Endpoint**: `http://localhost:8080/ws`

### STOMP Authentication
WebSocket connections are authenticated at the STOMP frame layer using JWT Bearer tokens during the initial `CONNECT` command.
Unauthenticated connection attempts, expired tokens, or malformed credentials are automatically rejected by `WebSocketAuthChannelInterceptor`.

#### STOMP CONNECT Frame Example
```
CONNECT
accept-version:1.2,1.1,1.0
heart-beat:10000,10000
Authorization:Bearer <JWT_ACCESS_TOKEN>

^@
```

#### Successful CONNECTED Response
```
CONNECTED
version:1.2
heart-beat:10000,10000
user-name:alice_j

^@
```

---

## 2. Destination Architecture

| Destination Pattern | Type | Access Authorization | Description |
|:---|:---|:---|:---|
| `/app/chat.send` | Inbound (Publish) | Authenticated | Sends a message (persisted first, then broadcasted) |
| `/app/chat.typing` | Inbound (Publish) | Authenticated | Sends a transient typing indicator (ephemeral, not persisted) |
| `/app/chat.delivered` | Inbound (Publish) | Authenticated | Acknowledges message delivery by recipient |
| `/app/chat.read` | Inbound (Publish) | Authenticated | Acknowledges message read by recipient |
| `/topic/conversations/{conversationPublicId}` | Outbound (Subscribe) | Conversation Member Only | Conversation broadcast for real-time messages |
| `/topic/conversations/{conversationPublicId}/typing` | Outbound (Subscribe) | Conversation Member Only | Conversation broadcast for typing events |
| `/user/queue/receipts` | Outbound (Subscribe) | Current User Only | Private delivery and read receipt notifications |

---

## 3. Real Payload & Message Examples

### A. Send Message (`/app/chat.send`)
Send a text message into a conversation. The sender identity is determined securely by the server from the authenticated principal; client-provided sender IDs are never trusted.

#### Outbound Client Frame (STOMP SEND)
```
SEND
destination:/app/chat.send
content-type:application/json

{
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "clientMessageId": "a904d9c4-1422-44df-9d5f-9e797a7a1ec6",
  "content": "Hello Bob! Nice to chat with you in real-time.",
  "messageType": "TEXT"
}
^@
```

#### Broadcast to Subscribers (`/topic/conversations/{conversationPublicId}`)
```
MESSAGE
destination:/topic/conversations/732e6423-a808-4e36-93d3-13ab18b958e0
subscription:sub-0
message-id:msg-101
content-type:application/json

{
  "publicId": "e1f10c66-4e55-472e-8e67-d8c9735d10a2",
  "clientMessageId": "a904d9c4-1422-44df-9d5f-9e797a7a1ec6",
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "senderPublicId": "550e8400-e29b-41d4-a716-446655440000",
  "senderUsername": "alice_j",
  "senderFullName": "Alice Johnson",
  "messageType": "TEXT",
  "content": "Hello Bob! Nice to chat with you in real-time.",
  "createdAt": "2026-11-26T18:40:00Z",
  "updatedAt": null,
  "edited": false,
  "deletedForEveryone": false
}
^@
```

---

### B. Typing Indicator (`/app/chat.typing`)
Typing indicators are ephemeral in-memory broadcasts that are not stored in the database.

#### Outbound Client Frame
```
SEND
destination:/app/chat.typing
content-type:application/json

{
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "typing": true
}
^@
```

#### Broadcast to Subscribers (`/topic/conversations/{conversationPublicId}/typing`)
```
MESSAGE
destination:/topic/conversations/732e6423-a808-4e36-93d3-13ab18b958e0/typing
subscription:sub-1
content-type:application/json

{
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "senderPublicId": "550e8400-e29b-41d4-a716-446655440000",
  "senderUsername": "alice_j",
  "typing": true
}
^@
```

---

### C. Delivery Receipt (`/app/chat.delivered`)
When a recipient client receives a message, it immediately acknowledges delivery.

#### Outbound Client Frame
```
SEND
destination:/app/chat.delivered
content-type:application/json

{
  "messagePublicId": "e1f10c66-4e55-472e-8e67-d8c9735d10a2"
}
^@
```

#### Sender Notification (`/user/queue/receipts`)
The original message sender receives a real-time receipt notification on their private queue:
```
MESSAGE
destination:/user/queue/receipts
subscription:sub-2
content-type:application/json

{
  "messagePublicId": "e1f10c66-4e55-472e-8e67-d8c9735d10a2",
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "recipientPublicId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "DELIVERED",
  "timestamp": "2026-11-26T18:40:02Z"
}
^@
```

---

## 4. Read Receipt (`/app/chat.read`)
When a user opens or focuses on a conversation, a read acknowledgment is dispatched. This updates all unread messages in that conversation, marks `readAt` on the `message_receipts` table, and resets the user's `unread_count` to `0`.

#### Outbound Client Frame
```
SEND
destination:/app/chat.read
content-type:application/json

{
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0"
}
^@
```

#### Sender Notification (`/user/queue/receipts`)
The sender receives a read acknowledgment for each read message:
```
MESSAGE
destination:/user/queue/receipts
subscription:sub-2
content-type:application/json

{
  "messagePublicId": "e1f10c66-4e55-472e-8e67-d8c9735d10a2",
  "conversationPublicId": "732e6423-a808-4e36-93d3-13ab18b958e0",
  "recipientPublicId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "READ",
  "timestamp": "2026-11-26T18:40:05Z"
}
^@
```

---

## 5. Presence Tracking

- **Connection Event**: Connect triggers `PresenceService.userConnected(username)`, incrementing the user's active session counter.
- **Disconnection Event**: Disconnect decrements session count. When all sessions terminate (session count = 0), `last_seen_at` is updated in the MySQL `users` table via `userRepository.save()`.
- Thread safety is guaranteed via `ConcurrentHashMap`.
