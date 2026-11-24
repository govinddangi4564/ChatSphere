import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import SettingsPage from '../SettingsPage';
import { AuthContext } from '../../context/AuthContext';
import { userService } from '../../services/userService';

vi.mock('../../services/userService', () => ({
  userService: {
    getPrivacySettings: vi.fn(),
    updatePrivacySettings: vi.fn(),
    getBlockedUsers: vi.fn(),
    unblockUser: vi.fn()
  }
}));

describe('SettingsPage', () => {
  const mockPrivacy = {
    profilePhotoVisibility: 'EVERYONE',
    lastSeenVisibility: 'CONTACTS_ONLY',
    onlineStatusVisibility: 'EVERYONE',
    messagePermission: 'EVERYONE'
  };

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
          <SettingsPage />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders and updates privacy settings', async () => {
    userService.getPrivacySettings.mockResolvedValueOnce(mockPrivacy);
    userService.updatePrivacySettings.mockResolvedValueOnce({
      ...mockPrivacy,
      profilePhotoVisibility: 'NOBODY'
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText(/who can see my profile photo/i)).toHaveValue('EVERYONE');
      expect(screen.getByLabelText(/who can see my last seen/i)).toHaveValue('CONTACTS_ONLY');
    });

    fireEvent.change(screen.getByLabelText(/who can see my profile photo/i), {
      target: { value: 'NOBODY' }
    });

    fireEvent.click(screen.getByRole('button', { name: /save privacy settings/i }));

    await waitFor(() => {
      expect(userService.updatePrivacySettings).toHaveBeenCalledWith({
        profilePhotoVisibility: 'NOBODY',
        lastSeenVisibility: 'CONTACTS_ONLY',
        onlineStatusVisibility: 'EVERYONE',
        messagePermission: 'EVERYONE'
      });
      expect(screen.getByText(/privacy settings saved successfully/i)).toBeInTheDocument();
    });
  });

  it('renders theme switcher and toggles dark mode', async () => {
    userService.getPrivacySettings.mockResolvedValueOnce(mockPrivacy);
    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /theme & display/i }));

    await waitFor(() => {
      expect(screen.getByText(/appearance & theme/i)).toBeInTheDocument();
    });

    const switchInput = screen.getByRole('switch');
    expect(switchInput).toBeInTheDocument();

    fireEvent.click(switchInput);
    expect(document.body.classList.contains('dark-mode')).toBe(true);
  });
});
