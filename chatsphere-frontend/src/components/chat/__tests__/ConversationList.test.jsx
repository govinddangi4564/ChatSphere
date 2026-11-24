import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ConversationList from '../ConversationList';

describe('ConversationList', () => {
  const mockConversations = [
    {
      publicId: 'conv-1',
      type: 'PRIVATE',
      title: 'Bob Jones',
      imageUrl: null,
      lastMessageAt: '2026-11-24T20:00:00Z',
      lastMessagePreview: 'Direct Message',
      unreadCount: 3,
      muted: false,
      archived: false,
      participant: { username: 'bobjones' }
    },
    {
      publicId: 'conv-2',
      type: 'PRIVATE',
      title: 'Alice Wonder',
      imageUrl: null,
      lastMessageAt: '2026-11-24T19:00:00Z',
      lastMessagePreview: 'Direct Message',
      unreadCount: 0,
      muted: true,
      archived: false,
      participant: { username: 'alicew' }
    }
  ];

  it('renders conversation list with titles and unread badges', () => {
    render(
      <ConversationList
        conversations={mockConversations}
        selectedId={null}
        onSelectConversation={vi.fn()}
        onOpenNewChatModal={vi.fn()}
        activeFilter="all"
        onChangeFilter={vi.fn()}
        loading={false}
      />
    );

    expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    expect(screen.getByText('Alice Wonder')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument(); // Unread badge
  });

  it('filters conversations by name or username', () => {
    render(
      <ConversationList
        conversations={mockConversations}
        selectedId={null}
        onSelectConversation={vi.fn()}
        onOpenNewChatModal={vi.fn()}
        activeFilter="all"
        onChangeFilter={vi.fn()}
        loading={false}
      />
    );

    const searchInput = screen.getByPlaceholderText(/search or start new chat/i);
    fireEvent.change(searchInput, { target: { value: 'alice' } });

    expect(screen.queryByText('Bob Jones')).not.toBeInTheDocument();
    expect(screen.getByText('Alice Wonder')).toBeInTheDocument();
  });

  it('calls onSelectConversation when item is clicked', () => {
    const handleSelect = vi.fn();
    render(
      <ConversationList
        conversations={mockConversations}
        selectedId={null}
        onSelectConversation={handleSelect}
        onOpenNewChatModal={vi.fn()}
        activeFilter="all"
        onChangeFilter={vi.fn()}
        loading={false}
      />
    );

    fireEvent.click(screen.getByText('Bob Jones'));
    expect(handleSelect).toHaveBeenCalledWith('conv-1');
  });
});
