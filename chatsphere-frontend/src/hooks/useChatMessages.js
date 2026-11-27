import { useState, useEffect, useRef, useCallback } from 'react';
import { messageService } from '../services/messageService';

export function useChatMessages(conversationPublicId, currentUser, webSocket, onMessageReceived) {
  const [messages, setMessages] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [partnerTyping, setPartnerTyping] = useState(false);

  const typingTimeoutRef = useRef(null);

  // Load first page of message history
  const loadInitialMessages = useCallback(async () => {
    if (!conversationPublicId) return;
    try {
      setLoadingInitial(true);
      const res = await messageService.getMessages(conversationPublicId, { size: 30 });
      // Map retrieved messages to local shape with initial status
      const initialMessages = (res.data || []).map((m) => ({
        ...m,
        status: m.senderUsername === currentUser?.username ? 'SENT' : undefined
      }));
      setMessages((prev) => {
        const pending = prev.filter((m) => m.status === 'SENDING' || m.status === 'FAILED');
        return [...initialMessages, ...pending];
      });
      setNextCursor(res.nextCursor);
      setHasMore(res.hasMore);

      // Trigger read receipt on open
      if (webSocket?.sendRead) {
        webSocket.sendRead(conversationPublicId);
      }
    } catch (e) {
      console.error('Failed to load initial messages:', e);
    } finally {
      setLoadingInitial(false);
    }
  }, [conversationPublicId, currentUser?.username, webSocket]);

  useEffect(() => {
    loadInitialMessages();
  }, [loadInitialMessages]);

  // Load older messages for infinite scroll upward
  const loadOlderMessages = useCallback(async () => {
    if (!conversationPublicId || !hasMore || loadingOlder || !nextCursor) {
      return false;
    }
    try {
      setLoadingOlder(true);
      const res = await messageService.getMessages(conversationPublicId, {
        size: 30,
        before: nextCursor
      });
      const olderMessages = (res.data || []).map((m) => ({
        ...m,
        status: m.senderUsername === currentUser?.username ? 'SENT' : undefined
      }));

      setMessages((prev) => [...olderMessages, ...prev]);
      setNextCursor(res.nextCursor);
      setHasMore(res.hasMore);
      return true;
    } catch (e) {
      console.error('Failed to load older messages:', e);
      return false;
    } finally {
      setLoadingOlder(false);
    }
  }, [conversationPublicId, hasMore, loadingOlder, nextCursor, currentUser?.username]);

  // Send message optimistically
  const sendMessage = useCallback(
    async (content, attachmentPublicIds = [], messageType = null, attachments = [], replyTo = null) => {
      const hasContent = content && content.trim();
      const hasAttachments = attachmentPublicIds && attachmentPublicIds.length > 0;
      if (!hasContent && !hasAttachments || !conversationPublicId) return;

      const clientMessageId = 'client-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
      const computedType = messageType || (hasAttachments ? (attachments[0]?.fileType || 'IMAGE') : 'TEXT');

      const optimisticMsg = {
        publicId: null,
        clientMessageId,
        conversationPublicId,
        senderPublicId: currentUser?.publicId,
        senderUsername: currentUser?.username,
        senderFullName: currentUser?.fullName,
        content: hasContent ? content.trim() : '',
        messageType: computedType,
        attachments: attachments || [],
        replyToMessage: replyTo,
        createdAt: new Date().toISOString(),
        status: 'SENDING'
      };

      setMessages((prev) => [...prev, optimisticMsg]);

      try {
        const payload = {
          conversationPublicId,
          clientMessageId,
          content: hasContent ? content.trim() : '',
          messageType: computedType,
          attachmentPublicIds: attachmentPublicIds || [],
          replyToMessagePublicId: replyTo?.publicId || null
        };
        const restResult = await webSocket.sendChatMessage(payload);

        // If REST fallback returned directly, reconcile immediately
        if (restResult && restResult.publicId) {
          setMessages((prev) =>
            prev.map((m) =>
              m.clientMessageId === clientMessageId
                ? { ...m, ...restResult, status: 'SENT' }
                : m
            )
          );
          if (onMessageReceived) {
            onMessageReceived({ ...restResult, conversationPublicId });
          }
        }
      } catch (err) {
        console.error('Send message failed:', err);
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMessageId === clientMessageId
              ? { ...m, status: 'FAILED' }
              : m
          )
        );
      }
    },
    [conversationPublicId, currentUser, webSocket, onMessageReceived]
  );

  // Retry sending a previously failed message
  const retryMessage = useCallback(
    async (clientMessageId) => {
      const msg = messages.find((m) => m.clientMessageId === clientMessageId);
      if (!msg) return;

      setMessages((prev) =>
        prev.map((m) =>
          m.clientMessageId === clientMessageId
            ? { ...m, status: 'SENDING' }
            : m
        )
      );

      try {
        const payload = {
          conversationPublicId,
          clientMessageId,
          content: msg.content,
          messageType: msg.messageType || 'TEXT',
          attachmentPublicIds: (msg.attachments || []).map((a) => a.publicId)
        };
        const restResult = await webSocket.sendChatMessage(payload);
        if (restResult && restResult.publicId) {
          setMessages((prev) =>
            prev.map((m) =>
              m.clientMessageId === clientMessageId
                ? { ...m, ...restResult, status: 'SENT' }
                : m
            )
          );
          if (onMessageReceived) {
            onMessageReceived({ ...restResult, conversationPublicId });
          }
        }
      } catch {
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMessageId === clientMessageId
              ? { ...m, status: 'FAILED' }
              : m
          )
        );
      }
    },
    [conversationPublicId, messages, webSocket, onMessageReceived]
  );

  // Send typing event
  const sendTypingIndicator = useCallback(
    (typing) => {
      if (webSocket?.sendTyping && conversationPublicId) {
        webSocket.sendTyping(conversationPublicId, typing);
      }
    },
    [conversationPublicId, webSocket]
  );

  // Reconcile incoming message from WebSocket broadcast
  const handleIncomingMessage = useCallback(
    (newMsg) => {
      if (!newMsg) return;

      // Handle custom WebSocket broadcast events (edit, delete, reaction, pin)
      if (newMsg.type === 'MESSAGE_EDITED' && newMsg.message) {
        setMessages((prev) =>
          prev.map((m) => (m.publicId === newMsg.message.publicId ? { ...m, ...newMsg.message } : m))
        );
        return;
      }

      if (newMsg.type === 'MESSAGE_DELETED') {
        setMessages((prev) =>
          prev.map((m) =>
            m.publicId === newMsg.messagePublicId
              ? { ...m, content: 'This message was deleted', deletedForEveryone: true }
              : m
          )
        );
        return;
      }

      if (newMsg.type === 'MESSAGE_REACTION' && newMsg.message) {
        setMessages((prev) =>
          prev.map((m) =>
            m.publicId === newMsg.message.publicId ? { ...m, reactions: newMsg.message.reactions } : m
          )
        );
        return;
      }

      if (newMsg.type === 'MESSAGE_PINNED' && newMsg.message) {
        setMessages((prev) =>
          prev.map((m) =>
            m.publicId === newMsg.message.publicId ? { ...m, isPinned: newMsg.message.isPinned } : m
          )
        );
        return;
      }

      if (newMsg.conversationPublicId !== conversationPublicId) return;

      setMessages((prev) => {
        // 1. Check if it matches an optimistic message by clientMessageId
        const existingIdx = prev.findIndex(
          (m) => m.clientMessageId && m.clientMessageId === newMsg.clientMessageId
        );
        if (existingIdx !== -1) {
          const updated = [...prev];
          updated[existingIdx] = {
            ...updated[existingIdx],
            ...newMsg,
            status: 'SENT'
          };
          return updated;
        }

        // 2. Check if already present by publicId to prevent duplicates
        if (newMsg.publicId && prev.some((m) => m.publicId === newMsg.publicId)) {
          return prev;
        }

        // 3. New message from partner or other participant
        return [...prev, { ...newMsg, status: 'SENT' }];
      });

      // Acknowledge delivery and read if message is from partner
      if (newMsg.senderUsername !== currentUser?.username) {
        if (webSocket?.sendDelivered && newMsg.publicId) {
          webSocket.sendDelivered(newMsg.publicId);
        }
        if (webSocket?.sendRead) {
          webSocket.sendRead(conversationPublicId);
        }
      }

      if (onMessageReceived) {
        onMessageReceived(newMsg);
      }
    },
    [conversationPublicId, currentUser?.username, webSocket, onMessageReceived]
  );

  // Reconcile receipt updates (DELIVERED, READ)
  const handleReceipt = useCallback(
    (receipt) => {
      if (!receipt || receipt.conversationPublicId !== conversationPublicId) return;
      setMessages((prev) =>
        prev.map((m) => {
          if (m.publicId === receipt.messagePublicId) {
            // Priority: READ overrides DELIVERED
            const newStatus = receipt.status === 'READ' ? 'READ' : (m.status === 'READ' ? 'READ' : 'DELIVERED');
            return { ...m, status: newStatus };
          }
          return m;
        })
      );
    },
    [conversationPublicId]
  );

  // Handle typing broadcast from partner
  const handleTypingEvent = useCallback(
    (event) => {
      if (!event || event.conversationPublicId !== conversationPublicId) return;
      if (event.senderUsername === currentUser?.username) return;

      if (event.typing) {
        setPartnerTyping(true);
        if (typingTimeoutRef.current) {
          clearTimeout(typingTimeoutRef.current);
        }
        typingTimeoutRef.current = setTimeout(() => {
          setPartnerTyping(false);
        }, 3000);
      } else {
        setPartnerTyping(false);
        if (typingTimeoutRef.current) {
          clearTimeout(typingTimeoutRef.current);
        }
      }
    },
    [conversationPublicId, currentUser?.username]
  );

  // Subscribe to conversation topics and private receipts
  useEffect(() => {
    if (!conversationPublicId || !webSocket?.subscribe) return;

    const unsubMsg = webSocket.subscribe(
      `/topic/conversations/${conversationPublicId}`,
      handleIncomingMessage
    );

    const unsubTyping = webSocket.subscribe(
      `/topic/conversations/${conversationPublicId}/typing`,
      handleTypingEvent
    );

    const unsubReceipts = webSocket.subscribe(
      '/user/queue/receipts',
      handleReceipt
    );

    return () => {
      unsubMsg();
      unsubTyping();
      unsubReceipts();
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, [conversationPublicId, webSocket, handleIncomingMessage, handleTypingEvent, handleReceipt]);

  return {
    messages,
    hasMore,
    loading: loadingInitial,
    loadingInitial,
    loadingMore: loadingOlder,
    loadingOlder,
    partnerTyping,
    loadMoreMessages: loadOlderMessages,
    loadOlderMessages,
    sendMessage,
    retryMessage,
    sendTypingIndicator
  };
}
