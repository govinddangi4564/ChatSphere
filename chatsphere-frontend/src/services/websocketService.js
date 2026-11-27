import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.token = null;
    this.connected = false;
    this.connectionPromise = null;
    this.subscriptions = new Map();
  }

  getWsUrl() {
    const isHttps = window.location.protocol === 'https:';
    const host = window.location.hostname || 'localhost';
    const port = 8080; // Backend port
    return `${isHttps ? 'wss:' : 'ws:'}//${host}:${port}/ws`;
  }

  getHttpWsUrl() {
    const isHttps = window.location.protocol === 'https:';
    const host = window.location.hostname || 'localhost';
    const port = 8080;
    return `${isHttps ? 'https:' : 'http:'}//${host}:${port}/ws`;
  }

  /**
   * Connect to STOMP broker over WebSocket with automatic reconnect and SockJS fallback
   * @param {string} token - JWT Access token
   * @param {() => void} [onConnectedCallback]
   * @param {(error: any) => void} [onErrorCallback]
   */
  connect(token, onConnectedCallback, onErrorCallback) {
    if (this.client && this.connected) {
      if (onConnectedCallback) onConnectedCallback();
      return;
    }

    this.token = token;

    this.client = new Client({
      brokerURL: this.getWsUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      // SockJS fallback factory if pure WebSocket connection fails
      webSocketFactory: () => {
        return new SockJS(this.getHttpWsUrl());
      },

      onConnect: () => {
        this.connected = true;
        // Resubscribe active subscriptions on reconnect
        for (const [destination, callbacks] of this.subscriptions.entries()) {
          this._bindSubscription(destination, callbacks);
        }
        if (onConnectedCallback) onConnectedCallback();
      },

      onDisconnect: () => {
        this.connected = false;
      },

      onStompError: (frame) => {
        console.error('STOMP Protocol Error:', frame.headers['message'], frame.body);
        if (onErrorCallback) onErrorCallback(frame);
      },

      onWebSocketError: (event) => {
        console.warn('WebSocket transport error, falling back to reconnect/SockJS:', event);
      }
    });

    this.client.activate();
  }

  _bindSubscription(destination, callbacks) {
    if (!this.client || !this.connected) return;
    try {
      const sub = this.client.subscribe(destination, (message) => {
        try {
          const payload = JSON.parse(message.body);
          callbacks.forEach((cb) => cb(payload));
        } catch (e) {
          console.error('Error parsing STOMP message:', e);
        }
      });
      return sub;
    } catch (e) {
      console.warn(`Failed to subscribe to ${destination}:`, e);
    }
  }

  /**
   * Subscribe to a destination with auto-cleanup callback
   * @param {string} destination
   * @param {(payload: any) => void} callback
   * @returns {() => void} unsubscribe function
   */
  subscribe(destination, callback) {
    if (!this.subscriptions.has(destination)) {
      this.subscriptions.set(destination, new Set());
    }
    const callbacks = this.subscriptions.get(destination);
    callbacks.add(callback);

    let stompSub = null;
    if (this.connected) {
      stompSub = this._bindSubscription(destination, callbacks);
    }

    return () => {
      callbacks.delete(callback);
      if (callbacks.size === 0) {
        this.subscriptions.delete(destination);
        if (stompSub) {
          try {
            stompSub.unsubscribe();
          } catch {
            // ignore
          }
        }
      }
    };
  }

  /**
   * Send a message to a STOMP destination
   * @param {string} destination
   * @param {object} payload
   */
  send(destination, payload) {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket client is not connected');
    }
    this.client.publish({
      destination,
      body: JSON.stringify(payload)
    });
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.clear();
      this.connected = false;
      this.client.deactivate();
      this.client = null;
    }
  }

  isConnected() {
    return this.connected;
  }
}

export const websocketService = new WebSocketService();
