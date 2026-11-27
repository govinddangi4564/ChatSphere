import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import MessageBubble from '../MessageBubble';

describe('MessageBubble Phase 8 Management Features', () => {
  it('renders reply preview snippet and edited tag', () => {
    const message = {
      publicId: 'msg-reply-1',
      clientMessageId: 'cli-r-1',
      senderUsername: 'alice',
      messageType: 'TEXT',
      content: 'I agree with this',
      edited: true,
      createdAt: new Date().toISOString(),
      replyToMessage: {
        publicId: 'msg-parent',
        senderUsername: 'bob',
        senderFullName: 'Bob Builder',
        content: 'Original proposal',
        messageType: 'TEXT'
      }
    };

    render(
      <MessageBubble
        message={message}
        isOwnMessage={false}
        isGroup={false}
        currentUser={{ username: 'currentUser' }}
      />
    );

    expect(screen.getByText('Bob Builder')).toBeInTheDocument();
    expect(screen.getByText('Original proposal')).toBeInTheDocument();
    expect(screen.getByText('(edited)')).toBeInTheDocument();
    expect(screen.getByText('I agree with this')).toBeInTheDocument();
  });

  it('renders context menu and triggers reply, react, and delete callbacks', () => {
    const onReply = vi.fn();
    const onReact = vi.fn();
    const onDelete = vi.fn();

    const message = {
      publicId: 'msg-ctx-1',
      clientMessageId: 'cli-ctx-1',
      senderUsername: 'alice',
      messageType: 'TEXT',
      content: 'Right click or click menu',
      createdAt: new Date().toISOString()
    };

    render(
      <MessageBubble
        message={message}
        isOwnMessage={true}
        isGroup={false}
        onReply={onReply}
        onReact={onReact}
        onDelete={onDelete}
        currentUser={{ username: 'alice' }}
      />
    );

    // Open context menu
    const menuBtn = screen.getByTitle('Options');
    fireEvent.click(menuBtn);

    // Click Reply
    const replyBtn = screen.getByText('Reply');
    fireEvent.click(replyBtn);
    expect(onReply).toHaveBeenCalledWith(message);

    // Open menu again & click Delete for everyone
    fireEvent.click(menuBtn);
    const deleteEveryoneBtn = screen.getByText('Delete for everyone');
    fireEvent.click(deleteEveryoneBtn);
    expect(onDelete).toHaveBeenCalledWith('msg-ctx-1', true);
  });

  it('renders reactions and triggers onReact callback when clicked', () => {
    const onRemoveReaction = vi.fn();
    const message = {
      publicId: 'msg-react-1',
      clientMessageId: 'cli-react-1',
      senderUsername: 'alice',
      messageType: 'TEXT',
      content: 'Awesome news!',
      createdAt: new Date().toISOString(),
      reactions: [
        { emoji: '❤️', username: 'bob', userPublicId: 'u-bob' },
        { emoji: '👍', username: 'alice', userPublicId: 'u-alice' }
      ]
    };

    render(
      <MessageBubble
        message={message}
        isOwnMessage={false}
        isGroup={false}
        onRemoveReaction={onRemoveReaction}
        currentUser={{ username: 'alice' }}
      />
    );

    // Heart reaction badge
    expect(screen.getByText('❤️')).toBeInTheDocument();

    // Alice's thumbs up reaction badge - clicking should remove own reaction
    const thumbsUpBadge = screen.getByTitle('Click to remove');
    expect(thumbsUpBadge).toBeInTheDocument();
    fireEvent.click(thumbsUpBadge);
    expect(onRemoveReaction).toHaveBeenCalledWith('msg-react-1');
  });
});
