import React, { useState, useEffect } from 'react';
import { contactService } from '../../services/contactService';
import { groupService } from '../../services/groupService';

export default function NewGroupModal({ show, onClose, onGroupCreated }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [contacts, setContacts] = useState([]);
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (show) {
      setTitle('');
      setDescription('');
      setImageUrl('');
      setSelectedMembers([]);
      setSearchQuery('');
      setError(null);
      loadContacts();
    }
  }, [show]);

  const loadContacts = async () => {
    try {
      setLoadingContacts(true);
      const res = await contactService.getContacts();
      const list = Array.isArray(res) ? res : res?.content || [];
      setContacts(list);
    } catch {
      setContacts([]);
    } finally {
      setLoadingContacts(false);
    }
  };

  if (!show) return null;

  const toggleSelectMember = (user) => {
    const isSelected = selectedMembers.some((m) => m.publicId === user.publicId);
    if (isSelected) {
      setSelectedMembers((prev) => prev.filter((m) => m.publicId !== user.publicId));
    } else {
      setSelectedMembers((prev) => [...prev, user]);
    }
  };

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      setError('Group title is required');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const payload = {
        title: trimmedTitle,
        description: description.trim() || null,
        imageUrl: imageUrl.trim() || null,
        memberPublicIds: selectedMembers.map((m) => m.publicId)
      };

      const newGroup = await groupService.createGroup(payload);
      if (onGroupCreated) {
        onGroupCreated(newGroup);
      }
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to create group';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const filteredContacts = contacts.filter((c) => {
    const u = c.contactUser || c.user || c;
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const nameMatch = u.fullName && u.fullName.toLowerCase().includes(q);
    const userMatch = u.username && u.username.toLowerCase().includes(q);
    return nameMatch || userMatch;
  });

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content border-0 shadow">
          <div className="modal-header bg-success text-white">
            <h5 className="modal-title fw-bold">New Group</h5>
            <button
              type="button"
              className="btn-close btn-close-white"
              onClick={onClose}
              disabled={submitting}
              aria-label="Close"
            ></button>
          </div>

          <form onSubmit={handleCreateGroup}>
            <div className="modal-body p-3">
              {error && <div className="alert alert-danger py-2 px-3 small mb-3">{error}</div>}

              {/* Group Name */}
              <div className="mb-3">
                <label className="form-label small fw-semibold">Group Title *</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. Project Alpha, Family, Tech Talk"
                  maxLength={100}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {/* Description */}
              <div className="mb-3">
                <label className="form-label small fw-semibold">Description (Optional)</label>
                <textarea
                  className="form-control"
                  rows={2}
                  placeholder="Group purpose or topic"
                  maxLength={1000}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>

              {/* Image URL */}
              <div className="mb-3">
                <label className="form-label small fw-semibold">Group Icon URL (Optional)</label>
                <input
                  type="url"
                  className="form-control form-control-sm"
                  placeholder="https://example.com/icon.png"
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                />
              </div>

              {/* Selected Members Chips */}
              {selectedMembers.length > 0 && (
                <div className="mb-3">
                  <label className="form-label small fw-semibold d-block">
                    Selected Members ({selectedMembers.length})
                  </label>
                  <div className="d-flex flex-wrap gap-1">
                    {selectedMembers.map((m) => (
                      <span
                        key={m.publicId}
                        className="badge bg-light text-dark border d-flex align-items-center gap-1 px-2 py-1"
                        style={{ fontSize: '0.8rem' }}
                      >
                        {m.fullName || m.username}
                        <button
                          type="button"
                          className="btn-close"
                          style={{ width: '8px', height: '8px' }}
                          onClick={() => toggleSelectMember(m)}
                          aria-label="Remove"
                        ></button>
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Contact Picker */}
              <div className="mb-2">
                <label className="form-label small fw-semibold">Add Participants from Contacts</label>
                <input
                  type="text"
                  className="form-control form-control-sm mb-2"
                  placeholder="Search contacts..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />

                <div
                  className="border rounded overflow-auto"
                  style={{ maxHeight: '180px', backgroundColor: '#fafafa' }}
                >
                  {loadingContacts ? (
                    <div className="text-center py-3">
                      <div className="spinner-border spinner-border-sm text-secondary" role="status"></div>
                    </div>
                  ) : filteredContacts.length === 0 ? (
                    <div className="text-center py-3 text-muted small">No contacts found</div>
                  ) : (
                    filteredContacts.map((c) => {
                      const u = c.contactUser || c.user || c;
                      const isSelected = selectedMembers.some((m) => m.publicId === u.publicId);
                      return (
                        <div
                          key={u.publicId}
                          className={`d-flex align-items-center justify-content-between p-2 border-bottom ${
                            isSelected ? 'bg-light' : ''
                          }`}
                          style={{ cursor: 'pointer' }}
                          onClick={() => toggleSelectMember(u)}
                        >
                          <div className="d-flex align-items-center gap-2">
                            <div
                              className="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold bg-secondary"
                              style={{ width: '32px', height: '32px', fontSize: '0.85rem' }}
                            >
                              {(u.fullName || u.username || 'U').charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <div className="fw-semibold small">{u.fullName || u.username}</div>
                              {u.fullName && <div className="text-muted" style={{ fontSize: '0.75rem' }}>@{u.username}</div>}
                            </div>
                          </div>
                          <input
                            type="checkbox"
                            className="form-check-input"
                            checked={isSelected}
                            onChange={() => {}} // Handled by div click
                          />
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer border-top-0 pt-0">
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={onClose}
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-success btn-sm px-3"
                disabled={!title.trim() || submitting}
              >
                {submitting ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-1" role="status"></span>
                    Creating...
                  </>
                ) : (
                  'Create Group'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
