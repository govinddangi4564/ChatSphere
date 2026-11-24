import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ProfilePage from '../ProfilePage';
import { AuthContext } from '../../context/AuthContext';
import { userService } from '../../services/userService';

vi.mock('../../services/userService', () => ({
  userService: {
    getMe: vi.fn(),
    updateProfile: vi.fn()
  }
}));

describe('ProfilePage', () => {
  const mockUser = {
    publicId: 'user-uuid-1',
    fullName: 'Alice Smith',
    username: 'alice',
    email: 'alice@example.com',
    profileImageUrl: 'https://example.com/alice.jpg',
    bio: 'Coding enthusiast',
    accountStatus: 'ACTIVE',
    emailVerified: true
  };

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <AuthContext.Provider
          value={{
            user: mockUser,
            isAuthenticated: true,
            logout: vi.fn()
          }}
        >
          <ProfilePage />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads and displays user profile data', async () => {
    userService.getMe.mockResolvedValueOnce(mockUser);
    renderComponent();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Alice Smith')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Coding enthusiast')).toBeInTheDocument();
      expect(screen.getByDisplayValue('https://example.com/alice.jpg')).toBeInTheDocument();
      expect(screen.getByDisplayValue('alice@example.com')).toBeInTheDocument();
    });
  });

  it('updates profile upon form submission', async () => {
    userService.getMe.mockResolvedValueOnce(mockUser);
    userService.updateProfile.mockResolvedValueOnce({
      ...mockUser,
      fullName: 'Alice Johnson',
      bio: 'New bio'
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Alice Smith')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/full name/i), {
      target: { value: 'Alice Johnson' }
    });
    fireEvent.change(screen.getByLabelText(/bio/i), {
      target: { value: 'New bio' }
    });

    fireEvent.click(screen.getByRole('button', { name: /save profile/i }));

    await waitFor(() => {
      expect(userService.updateProfile).toHaveBeenCalledWith({
        fullName: 'Alice Johnson',
        bio: 'New bio',
        profileImageUrl: 'https://example.com/alice.jpg'
      });
      expect(screen.getByText(/profile updated successfully/i)).toBeInTheDocument();
    });
  });
});
