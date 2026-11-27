import { useEffect, useState, useCallback } from 'react';
import { websocketService } from '../services/websocketService';
import { messageService } from '../services/messageService';
import { useAuth } from './useAuth';

export function useWebSocket() {
  const { token, user } = useAuth();
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!token) {
      websocketService.disconnect();
      setConnected(false);
      return;
    }

    websocketService.connect(
      token,
      () => setConnected(true),
      () => setConnected(false)
    );

    return () => {
      // Keep connected across conversation navigation; component unmount won't kill active socket
    };
  }, [token]);

  const sendChatMessage = useCallback(async (payload) => {
    if (websocketService.isConnected()) {
      try {
        websocketService.send('/app/chat.send', payload);
        return;
      } catch (e) {
        console.warn('STOMP send failed, attempting REST fallback:', e);
      }
    }
    // REST fallback if socket disconnected or publish fails
    return await messageService.sendMessage(payload);
  }, []);

  const sendTyping = useCallback((conversationPublicId, typing) => {
    if (websocketService.isConnected()) {
      try {
        websocketService.send('/app/chat.typing', { conversationPublicId, typing });
      } catch {
        // ignore
      }
    }
  }, []);

  const sendDelivered = useCallback((messagePublicId) => {
    if (websocketService.isConnected()) {
      try {
        websocketService.send('/app/chat.delivered', { messagePublicId });
      } catch {
        // ignore
      }
    }
  }, []);

  const sendRead = useCallback((conversationPublicId) => {
    if (websocketService.isConnected()) {
      try {
        websocketService.send('/app/chat.read', { conversationPublicId });
      } catch {
        // ignore
      }
    }
  }, []);

  const subscribe = useCallback((destination, callback) => {
    return websocketService.subscribe(destination, callback);
  }, []);

  return {
    connected,
    sendChatMessage,
    sendTyping,
    sendDelivered,
    sendRead,
    subscribe,
    user
  };
}
