# ChatSphere WebSocket Documentation

## Overview
Real-time messaging in ChatSphere will be implemented in Phase 5 using Spring WebSocket and STOMP over SockJS.

## Endpoint & Destination Specification (Phase 5 Roadmap)
- **Handshake Endpoint:** `/ws`
- **Application Destination Prefix:** `/app`
- **User Destination Prefix:** `/user`
- **Broker Destinations:** `/topic` (group broadcasts), `/queue` (private messages)

### Client Send Destinations
- `/app/chat.send`: Send text or media message
- `/app/chat.typing`: Emit typing indicator
- `/app/chat.read`: Acknowledge read receipt
- `/app/chat.delivered`: Acknowledge delivery receipt
- `/app/chat.reaction`: Add/update emoji reaction

### Client Subscription Destinations
- `/user/queue/messages`: Inbound direct messages
- `/user/queue/notifications`: Inbound personal notifications
- `/user/queue/receipts`: Message status updates (sent, delivered, read)
- `/topic/conversations/{conversationId}`: Inbound group conversation messages
