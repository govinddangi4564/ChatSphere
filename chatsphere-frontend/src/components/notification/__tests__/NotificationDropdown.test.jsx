import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import NotificationDropdown from '../NotificationDropdown';
import { notificationService } from '../../../services/notificationService';
import { websocketService } from '../../../services/websocketService';

vi.mock('../../../services/notificationService', () => ({
  notificationService: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn()
  }
}));

vi.mock('../../../services/websocketService', () => ({
  websocketService: {
    subscribe: vi.fn(() => vi.fn()),
    isConnected: vi.fn(() => true)
  }
}));

vi.mock('../../../hooks/useAuth', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: { username: 'alice', fullName: 'Alice Smith' }
  })
}));

describe('NotificationDropdown Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    notificationService.getUnreadCount.mockResolvedValue(2);
    notificationService.getNotifications.mockResolvedValue({
      content: [
        {
          publicId: 'n1',
          notificationType: 'NEW_MESSAGE',
          title: 'Bob Jones',
          message: 'Hello Alice',
          referenceId: 'conv-1',
          isRead: false,
          createdAt: new Date().toISOString()
        },
        {
          publicId: 'n2',
          notificationType: 'MEMBER_ADDED',
          title: 'Dev Team',
          message: 'Bob added you to group',
          referenceId: 'group-1',
          isRead: true,
          createdAt: new Date(Date.now() - 3600000).toISOString()
        }
      ]
    });
    notificationService.markAsRead.mockResolvedValue({});
    notificationService.markAllAsRead.mockResolvedValue({});
  });

  it('renders notification bell and displays unread badge count', async () => {
    render(<NotificationDropdown />);

    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  it('opens dropdown panel on click and displays notification list', async () => {
    render(<NotificationDropdown />);

    const bellBtn = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByText('Notifications')).toBeInTheDocument();
      expect(screen.getByText('Bob Jones')).toBeInTheDocument();
      expect(screen.getByText('Hello Alice')).toBeInTheDocument();
      expect(screen.getByText('Dev Team')).toBeInTheDocument();
    });
  });

  it('marks single notification as read on click', async () => {
    render(<NotificationDropdown />);

    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    await waitFor(() => {
      expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    });

    const item = screen.getByText('Hello Alice').closest('.list-group-item');
    fireEvent.click(item);

    expect(notificationService.markAsRead).toHaveBeenCalledWith('n1');
  });

  it('calls markAllAsRead when "Mark all as read" button is clicked', async () => {
    render(<NotificationDropdown />);

    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    await waitFor(() => {
      expect(screen.getByText(/mark all as read/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/mark all as read/i));

    expect(notificationService.markAllAsRead).toHaveBeenCalled();
  });
});
