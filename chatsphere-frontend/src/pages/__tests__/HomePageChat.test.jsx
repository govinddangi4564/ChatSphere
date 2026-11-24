import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import HomePage from '../HomePage';
import { AuthContext } from '../../context/AuthContext';
import { conversationService } from '../../services/conversationService';

vi.mock('../../services/conversationService', () => ({
  conversationService: {
    getConversations: vi.fn(),
    getConversation: vi.fn(),
    createOrFindPrivateConversation: vi.fn(),
    archiveConversation: vi.fn(),
    muteConversation: vi.fn()
  }
}));

describe('HomePage Chat Layout', () => {
  const mockConversations = [
    {
      publicId: 'conv-1',
      type: 'PRIVATE',
      title: 'Charlie Chaplin',
      imageUrl: null,
      lastMessageAt: '2026-11-24T20:00:00Z',
      unreadCount: 0,
      muted: false,
      archived: false,
      participant: { username: 'charlie' }
    }
  ];

  const mockDetails = {
    publicId: 'conv-1',
    type: 'PRIVATE',
    title: 'Charlie Chaplin',
    imageUrl: null,
    createdAt: '2026-11-24T18:00:00Z',
    lastMessageAt: null,
    participants: [
      { publicId: 'u-1', fullName: 'Alice', username: 'alice', role: 'MEMBER' },
      { publicId: 'u-2', fullName: 'Charlie Chaplin', username: 'charlie', role: 'MEMBER' }
    ],
    muted: false,
    archived: false,
    unreadCount: 0,
    role: 'MEMBER'
  };

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <AuthContext.Provider
          value={{
            user: { username: 'alice', fullName: 'Alice Smith' },
            isAuthenticated: true,
            logout: vi.fn()
          }}
        >
          <HomePage />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders chat layout with conversations and default empty state', async () => {
    conversationService.getConversations.mockResolvedValueOnce({
      content: mockConversations,
      pagination: { totalElements: 1 }
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Charlie Chaplin')).toBeInTheDocument();
      expect(screen.getByText(/chatsphere web/i)).toBeInTheDocument();
    });
  });

  it('loads conversation details and displays chat area upon selection', async () => {
    conversationService.getConversations.mockResolvedValueOnce({
      content: mockConversations,
      pagination: { totalElements: 1 }
    });
    conversationService.getConversation.mockResolvedValueOnce(mockDetails);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Charlie Chaplin')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Charlie Chaplin'));

    await waitFor(() => {
      expect(conversationService.getConversation).toHaveBeenCalledWith('conv-1');
      expect(screen.getByText(/beginning of conversation/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/messaging will be enabled in phase 5/i)).toBeInTheDocument();
    });
  });
});
