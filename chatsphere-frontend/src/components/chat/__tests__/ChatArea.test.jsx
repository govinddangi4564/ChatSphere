import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ChatArea from '../ChatArea';
import { AuthContext } from '../../../context/AuthContext';
import { messageService } from '../../../services/messageService';

vi.mock('../../../services/messageService', () => ({
  messageService: {
    getMessages: vi.fn(),
    sendMessage: vi.fn()
  }
}));

describe('ChatArea Component', () => {
  const mockUser = {
    publicId: 'user-alice-id',
    username: 'alice',
    fullName: 'Alice Smith'
  };

  const mockConversation = {
    publicId: 'conv-123',
    title: 'Bob Jones',
    type: 'PRIVATE',
    otherParticipant: {
      publicId: 'user-bob-id',
      username: 'bob',
      fullName: 'Bob Jones',
      isOnline: true
    },
    muted: false,
    archived: false
  };

  const mockWebSocket = {
    subscribe: vi.fn(() => vi.fn()),
    sendChatMessage: vi.fn(),
    sendTyping: vi.fn(),
    sendDelivered: vi.fn(),
    sendRead: vi.fn(),
    isConnected: vi.fn(() => true)
  };

  const renderComponent = (props = {}) => {
    return render(
      <AuthContext.Provider
        value={{
          user: mockUser,
          isAuthenticated: true,
          token: 'mock-jwt-token'
        }}
      >
        <ChatArea
          conversation={mockConversation}
          webSocket={mockWebSocket}
          {...props}
        />
      </AuthContext.Provider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
    window.HTMLElement.prototype.scrollIntoView = vi.fn();
    mockWebSocket.subscribe.mockImplementation(() => vi.fn());
  });

  it('renders empty state placeholder when no conversation is selected', () => {
    render(
      <AuthContext.Provider value={{ user: mockUser, isAuthenticated: true }}>
        <ChatArea conversation={null} />
      </AuthContext.Provider>
    );

    expect(screen.getByText(/ChatSphere Web/i)).toBeInTheDocument();
    expect(screen.getByText(/Select a conversation from the sidebar/i)).toBeInTheDocument();
  });

  it('renders chat header and loads message history on mount', async () => {
    messageService.getMessages.mockResolvedValueOnce({
      success: true,
      data: [
        {
          publicId: 'msg-1',
          conversationPublicId: 'conv-123',
          senderPublicId: 'user-bob-id',
          senderUsername: 'bob',
          senderFullName: 'Bob Jones',
          content: 'Hello Alice!',
          messageType: 'TEXT',
          createdAt: '2026-11-27T10:00:00Z'
        },
        {
          publicId: 'msg-2',
          conversationPublicId: 'conv-123',
          senderPublicId: 'user-alice-id',
          senderUsername: 'alice',
          senderFullName: 'Alice Smith',
          content: 'Hi Bob, how are you?',
          messageType: 'TEXT',
          createdAt: '2026-11-27T10:01:00Z'
        }
      ],
      nextCursor: 'next-cursor-abc',
      hasMore: true
    });

    renderComponent();

    // Check header
    expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    expect(screen.getByText('online')).toBeInTheDocument();

    // Check messages
    await waitFor(() => {
      expect(screen.getByText('Hello Alice!')).toBeInTheDocument();
      expect(screen.getByText('Hi Bob, how are you?')).toBeInTheDocument();
    });

    // Check WebSocket read receipt sent on initial open
    expect(mockWebSocket.sendRead).toHaveBeenCalledWith('conv-123');
  });

  it('handles optimistic send with input clear and subscription acknowledgment', async () => {
    messageService.getMessages.mockResolvedValueOnce({
      success: true,
      data: [],
      nextCursor: null,
      hasMore: false
    });

    mockWebSocket.sendChatMessage.mockResolvedValueOnce({
      publicId: 'msg-server-assigned',
      conversationPublicId: 'conv-123',
      senderPublicId: 'user-alice-id',
      senderUsername: 'alice',
      senderFullName: 'Alice Smith',
      content: 'Optimistic test message',
      messageType: 'TEXT',
      createdAt: new Date().toISOString()
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/no messages yet/i)).toBeInTheDocument();
    });

    const input = screen.getByPlaceholderText(/type a message/i);
    fireEvent.change(input, { target: { value: 'Optimistic test message' } });
    expect(input.value).toBe('Optimistic test message');

    const form = input.closest('form');
    fireEvent.submit(form);

    // Input should be cleared immediately
    expect(input.value).toBe('');

    // Message should be rendered optimistically
    await waitFor(() => {
      expect(screen.getByText('Optimistic test message')).toBeInTheDocument();
    });

    expect(mockWebSocket.sendChatMessage).toHaveBeenCalledWith(
      expect.objectContaining({
        conversationPublicId: 'conv-123',
        content: 'Optimistic test message',
        messageType: 'TEXT'
      })
    );
  });

  it('shows retry button on failed send and retries successfully', async () => {
    messageService.getMessages.mockResolvedValueOnce({
      success: true,
      data: [],
      nextCursor: null,
      hasMore: false
    });

    // First send fails
    mockWebSocket.sendChatMessage.mockRejectedValueOnce(new Error('Network error'));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/no messages yet/i)).toBeInTheDocument();
    });

    const input = screen.getByPlaceholderText(/type a message/i);
    fireEvent.change(input, { target: { value: 'Failed message' } });
    const form = input.closest('form');
    fireEvent.submit(form);

    // Expect retry button to appear
    await waitFor(() => {
      expect(screen.getByTitle('Failed to send. Click to retry.')).toBeInTheDocument();
    });

    // Now mock success for retry
    mockWebSocket.sendChatMessage.mockResolvedValueOnce({
      publicId: 'msg-retry-success',
      conversationPublicId: 'conv-123',
      senderPublicId: 'user-alice-id',
      senderUsername: 'alice',
      content: 'Failed message',
      messageType: 'TEXT',
      createdAt: new Date().toISOString()
    });

    const retryBtn = screen.getByTitle('Failed to send. Click to retry.');
    fireEvent.click(retryBtn);

    // Expect retry call
    await waitFor(() => {
      expect(mockWebSocket.sendChatMessage).toHaveBeenCalledTimes(2);
    });
  });

  it('triggers pagination when user scrolls to top of message list', async () => {
    messageService.getMessages
      .mockResolvedValueOnce({
        success: true,
        data: [
          {
            publicId: 'msg-newer',
            conversationPublicId: 'conv-123',
            senderPublicId: 'user-bob-id',
            senderUsername: 'bob',
            content: 'Newer message',
            createdAt: '2026-11-27T12:00:00Z'
          }
        ],
        nextCursor: 'cursor-page-2',
        hasMore: true
      })
      .mockResolvedValueOnce({
        success: true,
        data: [
          {
            publicId: 'msg-older',
            conversationPublicId: 'conv-123',
            senderPublicId: 'user-bob-id',
            senderUsername: 'bob',
            content: 'Older message from history',
            createdAt: '2026-11-27T10:00:00Z'
          }
        ],
        nextCursor: null,
        hasMore: false
      });

    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Newer message')).toBeInTheDocument();
    });

    // Simulate scrolling upward
    const scrollContainer = container.querySelector('.chat-messages-container');
    expect(scrollContainer).toBeInTheDocument();

    // Trigger scroll event at top (scrollTop <= 60)
    scrollContainer.scrollTop = 10;
    fireEvent.scroll(scrollContainer);

    await waitFor(() => {
      expect(messageService.getMessages).toHaveBeenCalledWith('conv-123', {
        size: 30,
        before: 'cursor-page-2'
      });
      expect(screen.getByText('Older message from history')).toBeInTheDocument();
    });
  });

  it('renders group conversation header, system messages and incoming sender names', async () => {
    const mockGroupConv = {
      publicId: 'grp-test-1',
      title: 'Beta Testers',
      type: 'GROUP',
      participants: [
        { publicId: 'user-alice-id', fullName: 'Alice Smith', username: 'alice', role: 'ADMIN' },
        { publicId: 'user-bob-id', fullName: 'Bob Jones', username: 'bob', role: 'MEMBER' }
      ]
    };

    messageService.getMessages.mockResolvedValueOnce({
      success: true,
      data: [
        {
          publicId: 'msg-sys-1',
          conversationPublicId: 'grp-test-1',
          senderPublicId: 'user-alice-id',
          senderUsername: 'alice',
          senderFullName: 'Alice Smith',
          content: 'Alice created group "Beta Testers"',
          messageType: 'SYSTEM',
          createdAt: '2026-11-28T09:00:00Z'
        },
        {
          publicId: 'msg-group-bob',
          conversationPublicId: 'grp-test-1',
          senderPublicId: 'user-bob-id',
          senderUsername: 'bob',
          senderFullName: 'Bob Jones',
          content: 'Hey beta group members!',
          messageType: 'TEXT',
          createdAt: '2026-11-28T09:05:00Z'
        }
      ],
      nextCursor: null,
      hasMore: false
    });

    render(
      <AuthContext.Provider
        value={{
          user: mockUser,
          isAuthenticated: true,
          token: 'mock-jwt-token'
        }}
      >
        <ChatArea
          conversation={mockGroupConv}
          webSocket={mockWebSocket}
        />
      </AuthContext.Provider>
    );

    // Group Header assertions
    expect(screen.getByText('Beta Testers')).toBeInTheDocument();
    expect(screen.getByTitle('Group Info')).toBeInTheDocument();

    await waitFor(() => {
      // System message assertion
      expect(screen.getByText('Alice created group "Beta Testers"')).toBeInTheDocument();
      // Sender name above bubble assertion
      expect(screen.getByText('Bob Jones')).toBeInTheDocument();
      expect(screen.getByText('Hey beta group members!')).toBeInTheDocument();
    });
  });
});
