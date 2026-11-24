import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ContactsPage from '../ContactsPage';
import { AuthContext } from '../../context/AuthContext';
import { contactService } from '../../services/contactService';
import { userService } from '../../services/userService';

vi.mock('../../services/contactService', () => ({
  contactService: {
    getContacts: vi.fn(),
    addContact: vi.fn(),
    removeContact: vi.fn()
  }
}));

vi.mock('../../services/userService', () => ({
  userService: {
    searchUsers: vi.fn(),
    blockUser: vi.fn()
  }
}));

describe('ContactsPage', () => {
  const mockContacts = [
    {
      publicId: 'contact-uuid-1',
      fullName: 'Bob Builder',
      username: 'bob',
      profileImageUrl: null,
      bio: 'Can we fix it?'
    }
  ];

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <AuthContext.Provider
          value={{
            user: { username: 'alice', fullName: 'Alice' },
            isAuthenticated: true,
            logout: vi.fn()
          }}
        >
          <ContactsPage />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders contacts list', async () => {
    contactService.getContacts.mockResolvedValueOnce(mockContacts);
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Bob Builder')).toBeInTheDocument();
      expect(screen.getByText('@bob')).toBeInTheDocument();
      expect(screen.getByText('Can we fix it?')).toBeInTheDocument();
    });
  });

  it('allows searching users and adding a contact', async () => {
    contactService.getContacts.mockResolvedValueOnce([]);
    userService.searchUsers.mockResolvedValueOnce({
      content: [
        {
          publicId: 'target-uuid-2',
          fullName: 'Charlie Chaplin',
          username: 'charlie',
          bio: 'Actor',
          isContact: false
        }
      ],
      pagination: { totalPages: 1 }
    });
    contactService.addContact.mockResolvedValueOnce({});

    renderComponent();

    // Switch to search tab
    fireEvent.click(screen.getByRole('button', { name: /find people/i }));

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/search users by name or username/i)).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText('Charlie Chaplin')).toBeInTheDocument();
      expect(screen.getByText('@charlie')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /add/i });
    fireEvent.click(addButton);

    await waitFor(() => {
      expect(contactService.addContact).toHaveBeenCalledWith('target-uuid-2');
      expect(screen.getByText(/contact added successfully/i)).toBeInTheDocument();
    });
  });
});
