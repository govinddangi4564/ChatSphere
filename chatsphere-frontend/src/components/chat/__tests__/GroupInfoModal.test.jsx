import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import GroupInfoModal from '../GroupInfoModal';
import { groupService } from '../../../services/groupService';
import { AuthContext } from '../../../context/AuthContext';

vi.mock('../../../services/groupService', () => ({
  groupService: {
    getGroupDetails: vi.fn(),
    updateGroup: vi.fn(),
    addMembers: vi.fn(),
    removeMember: vi.fn(),
    leaveGroup: vi.fn(),
    updateMemberRole: vi.fn()
  }
}));

describe('GroupInfoModal Component', () => {
  const mockAdminUser = {
    publicId: 'u-alice',
    username: 'alice',
    fullName: 'Alice Admin'
  };

  const mockGroupDetails = {
    publicId: 'grp-123',
    title: 'Alpha Team',
    description: 'Alpha team discussion room',
    imageUrl: null,
    createdAt: '2026-11-28T10:00:00Z',
    createdBy: {
      publicId: 'u-alice',
      username: 'alice',
      fullName: 'Alice Admin'
    },
    currentUserRole: 'ADMIN',
    members: [
      {
        publicId: 'u-alice',
        username: 'alice',
        fullName: 'Alice Admin',
        role: 'ADMIN',
        joinedAt: '2026-11-28T10:00:00Z'
      },
      {
        publicId: 'u-bob',
        username: 'bob',
        fullName: 'Bob Jones',
        role: 'MEMBER',
        joinedAt: '2026-11-28T10:05:00Z'
      }
    ]
  };

  const renderComponent = (props = {}) => {
    return render(
      <AuthContext.Provider
        value={{
          user: mockAdminUser,
          isAuthenticated: true
        }}
      >
        <GroupInfoModal
          show={true}
          groupPublicId="grp-123"
          onClose={vi.fn()}
          {...props}
        />
      </AuthContext.Provider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
    groupService.getGroupDetails.mockResolvedValue(mockGroupDetails);
  });

  it('renders group info with title, description, and members list', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Alpha Team')).toBeInTheDocument();
      expect(screen.getByText('Alpha team discussion room')).toBeInTheDocument();
      expect(screen.getAllByText(/2 participants/i).length).toBeGreaterThan(0);
      expect(screen.getByText('Bob Jones')).toBeInTheDocument();
      expect(screen.getByText('Group Admin')).toBeInTheDocument();
    });
  });

  it('allows admin to edit group title and save changes', async () => {
    const onGroupUpdated = vi.fn();
    groupService.updateGroup.mockResolvedValueOnce({
      ...mockGroupDetails,
      title: 'Alpha Team Renamed'
    });

    renderComponent({ onGroupUpdated });

    await waitFor(() => {
      expect(screen.getByText('Alpha Team')).toBeInTheDocument();
    });

    // Click Edit Group
    fireEvent.click(screen.getByRole('button', { name: /edit group/i }));

    const titleInput = screen.getByDisplayValue('Alpha Team');
    fireEvent.change(titleInput, { target: { value: 'Alpha Team Renamed' } });

    // Click Save
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(groupService.updateGroup).toHaveBeenCalledWith('grp-123', expect.objectContaining({
        title: 'Alpha Team Renamed'
      }));
      expect(onGroupUpdated).toHaveBeenCalled();
    });
  });

  it('handles leaving the group with confirmation', async () => {
    const onLeaveGroup = vi.fn();
    const onClose = vi.fn();
    groupService.leaveGroup.mockResolvedValueOnce({});

    renderComponent({ onLeaveGroup, onClose });

    await waitFor(() => {
      expect(screen.getByText(/leave group/i)).toBeInTheDocument();
    });

    // Click Leave Group
    fireEvent.click(screen.getByRole('button', { name: /leave group/i }));

    // Confirm dialog appears
    expect(screen.getByText(/Are you sure you want to leave this group\?/i)).toBeInTheDocument();

    // Click Yes, Leave
    fireEvent.click(screen.getByRole('button', { name: /yes, leave/i }));

    await waitFor(() => {
      expect(groupService.leaveGroup).toHaveBeenCalledWith('grp-123');
      expect(onLeaveGroup).toHaveBeenCalledWith('grp-123');
      expect(onClose).toHaveBeenCalled();
    });
  });
});
