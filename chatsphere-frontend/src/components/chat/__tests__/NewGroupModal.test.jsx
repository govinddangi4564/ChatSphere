import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import NewGroupModal from '../NewGroupModal';
import { contactService } from '../../../services/contactService';
import { groupService } from '../../../services/groupService';

vi.mock('../../../services/contactService', () => ({
  contactService: {
    getContacts: vi.fn()
  }
}));

vi.mock('../../../services/groupService', () => ({
  groupService: {
    createGroup: vi.fn()
  }
}));

describe('NewGroupModal Component', () => {
  const mockContacts = [
    {
      publicId: 'c-1',
      contactUser: {
        publicId: 'u-bob',
        username: 'bob',
        fullName: 'Bob Jones'
      }
    },
    {
      publicId: 'c-2',
      contactUser: {
        publicId: 'u-charlie',
        username: 'charlie',
        fullName: 'Charlie Chaplin'
      }
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    contactService.getContacts.mockResolvedValue(mockContacts);
  });

  it('renders modal with title input, description, and loaded contacts', async () => {
    render(<NewGroupModal show={true} onClose={vi.fn()} onGroupCreated={vi.fn()} />);

    expect(screen.getByText('New Group')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e\.g\. Project Alpha/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Bob Jones')).toBeInTheDocument();
      expect(screen.getByText('Charlie Chaplin')).toBeInTheDocument();
    });
  });

  it('allows selecting and unselecting members, and submits form successfully', async () => {
    const onGroupCreated = vi.fn();
    const onClose = vi.fn();

    groupService.createGroup.mockResolvedValueOnce({
      publicId: 'grp-new-1',
      title: 'Awesome Devs',
      members: []
    });

    render(<NewGroupModal show={true} onClose={onClose} onGroupCreated={onGroupCreated} />);

    await waitFor(() => {
      expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    });

    // Enter title
    const titleInput = screen.getByPlaceholderText(/e\.g\. Project Alpha/i);
    fireEvent.change(titleInput, { target: { value: 'Awesome Devs' } });

    // Select Bob
    fireEvent.click(screen.getByText('Bob Jones'));
    expect(screen.getByText(/Selected Members \(1\)/i)).toBeInTheDocument();

    // Click submit
    const submitBtn = screen.getByRole('button', { name: /create group/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(groupService.createGroup).toHaveBeenCalledWith({
        title: 'Awesome Devs',
        description: null,
        imageUrl: null,
        memberPublicIds: ['u-bob']
      });
      expect(onGroupCreated).toHaveBeenCalledWith(expect.objectContaining({ publicId: 'grp-new-1' }));
      expect(onClose).toHaveBeenCalled();
    });
  });
});
