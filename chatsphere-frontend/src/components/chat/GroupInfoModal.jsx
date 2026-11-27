import React, { useState, useEffect, useCallback } from 'react';
import { groupService } from '../../services/groupService';
import { contactService } from '../../services/contactService';
import { useAuth } from '../../hooks/useAuth';

export default function GroupInfoModal({ show, onClose, groupPublicId, onGroupUpdated, onLeaveGroup }) {
  const { user: currentUser } = useAuth();
  const [group, setGroup] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Edit group info state
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editImageUrl, setEditImageUrl] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  // Add members state
  const [showAddMembers, setShowAddMembers] = useState(false);
  const [contacts, setContacts] = useState([]);
  const [selectedToAdd, setSelectedToAdd] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [addingMembers, setAddingMembers] = useState(false);

  // Action loading state per user
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [confirmLeave, setConfirmLeave] = useState(false);
  const [leaving, setLeaving] = useState(false);

  const loadGroupDetails = useCallback(async () => {
    if (!groupPublicId) return;
    try {
      setLoading(true);
      setError(null);
      const data = await groupService.getGroupDetails(groupPublicId);
      setGroup(data);
      setEditTitle(data.title || '');
      setEditDescription(data.description || '');
      setEditImageUrl(data.imageUrl || '');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load group details');
    } finally {
      setLoading(false);
    }
  }, [groupPublicId]);

  useEffect(() => {
    if (show && groupPublicId) {
      setEditing(false);
      setShowAddMembers(false);
      setConfirmLeave(false);
      loadGroupDetails();
    }
  }, [show, groupPublicId, loadGroupDetails]);

  const loadContactsForAdd = async () => {
    try {
      setLoadingContacts(true);
      const res = await contactService.getContacts();
      const list = Array.isArray(res) ? res : res?.content || [];
      // Filter out users who are already members
      const currentMemberIds = new Set((group?.members || []).map((m) => m.publicId));
      const available = list.filter((c) => {
        const u = c.contactUser || c.user || c;
        return !currentMemberIds.has(u.publicId);
      });
      setContacts(available);
      setSelectedToAdd([]);
    } catch {
      setContacts([]);
    } finally {
      setLoadingContacts(false);
    }
  };

  const handleToggleAddMembers = () => {
    if (!showAddMembers) {
      loadContactsForAdd();
    }
    setShowAddMembers(!showAddMembers);
  };

  const toggleSelectToAdd = (u) => {
    const isSelected = selectedToAdd.some((m) => m.publicId === u.publicId);
    if (isSelected) {
      setSelectedToAdd((prev) => prev.filter((m) => m.publicId !== u.publicId));
    } else {
      setSelectedToAdd((prev) => [...prev, u]);
    }
  };

  const handleAddMembersSubmit = async () => {
    if (selectedToAdd.length === 0) return;
    try {
      setAddingMembers(true);
      const memberIds = selectedToAdd.map((m) => m.publicId);
      const updated = await groupService.addMembers(groupPublicId, memberIds);
      setGroup(updated);
      setShowAddMembers(false);
      setSelectedToAdd([]);
      if (onGroupUpdated) onGroupUpdated(updated);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add members');
    } finally {
      setAddingMembers(false);
    }
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    if (!editTitle.trim()) return;

    try {
      setSavingEdit(true);
      const updated = await groupService.updateGroup(groupPublicId, {
        title: editTitle.trim(),
        description: editDescription.trim() || null,
        imageUrl: editImageUrl.trim() || null
      });
      setGroup(updated);
      setEditing(false);
      if (onGroupUpdated) onGroupUpdated(updated);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update group');
    } finally {
      setSavingEdit(false);
    }
  };

  const handleRoleToggle = async (memberPublicId, currentRole) => {
    const newRole = currentRole === 'ADMIN' ? 'MEMBER' : 'ADMIN';
    try {
      setActionLoadingId(memberPublicId);
      const updated = await groupService.updateMemberRole(groupPublicId, memberPublicId, newRole);
      setGroup(updated);
      if (onGroupUpdated) onGroupUpdated(updated);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update role');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleRemoveMember = async (memberPublicId) => {
    try {
      setActionLoadingId(memberPublicId);
      await groupService.removeMember(groupPublicId, memberPublicId);
      setGroup((prev) => ({
        ...prev,
        members: prev.members.filter((m) => m.publicId !== memberPublicId)
      }));
      if (onGroupUpdated) {
        onGroupUpdated({
          ...group,
          members: group.members.filter((m) => m.publicId !== memberPublicId)
        });
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to remove member');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleLeaveGroup = async () => {
    try {
      setLeaving(true);
      await groupService.leaveGroup(groupPublicId);
      if (onLeaveGroup) onLeaveGroup(groupPublicId);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to leave group');
    } finally {
      setLeaving(false);
    }
  };

  if (!show) return null;

  const isAdmin = group?.currentUserRole === 'ADMIN';

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-dialog-scrollable">
        <div className="modal-content border-0 shadow">
          {/* Header */}
          <div className="modal-header bg-success text-white">
            <h5 className="modal-title fw-bold">Group Info</h5>
            <button
              type="button"
              className="btn-close btn-close-white"
              onClick={onClose}
              aria-label="Close"
            ></button>
          </div>

          <div className="modal-body p-3">
            {error && <div className="alert alert-danger py-2 px-3 small mb-3">{error}</div>}

            {loading ? (
              <div className="text-center py-5">
                <div className="spinner-border text-success" role="status"></div>
              </div>
            ) : group ? (
              <>
                {/* Group Profile Header */}
                <div className="text-center pb-3 border-bottom">
                  <div
                    className="rounded-circle d-inline-flex align-items-center justify-content-center text-white fw-bold bg-secondary mb-2 shadow-sm"
                    style={{ width: '72px', height: '72px', fontSize: '1.8rem' }}
                  >
                    {group.imageUrl ? (
                      <img
                        src={group.imageUrl}
                        alt={group.title}
                        className="rounded-circle w-100 h-100"
                        style={{ objectFit: 'cover' }}
                      />
                    ) : (
                      group.title.charAt(0).toUpperCase()
                    )}
                  </div>

                  {!editing ? (
                    <>
                      <h5 className="fw-bold mb-1">{group.title}</h5>
                      <p className="text-muted small mb-1">
                        Group · {group.members?.length || 0} participants
                      </p>
                      {group.description && (
                        <p className="small text-secondary px-3 mb-2">{group.description}</p>
                      )}
                      {isAdmin && (
                        <button
                          className="btn btn-sm btn-outline-success rounded-pill px-3 py-1"
                          onClick={() => setEditing(true)}
                        >
                          <i className="bi bi-pencil me-1"></i> Edit Group
                        </button>
                      )}
                    </>
                  ) : (
                    <form onSubmit={handleSaveEdit} className="text-start mt-2 px-2">
                      <div className="mb-2">
                        <label className="form-label small fw-semibold">Group Title *</label>
                        <input
                          type="text"
                          className="form-control form-control-sm"
                          value={editTitle}
                          onChange={(e) => setEditTitle(e.target.value)}
                          required
                        />
                      </div>
                      <div className="mb-2">
                        <label className="form-label small fw-semibold">Description</label>
                        <textarea
                          className="form-control form-control-sm"
                          rows={2}
                          value={editDescription}
                          onChange={(e) => setEditDescription(e.target.value)}
                        />
                      </div>
                      <div className="mb-2">
                        <label className="form-label small fw-semibold">Image URL</label>
                        <input
                          type="url"
                          className="form-control form-control-sm"
                          value={editImageUrl}
                          onChange={(e) => setEditImageUrl(e.target.value)}
                        />
                      </div>
                      <div className="d-flex gap-2 justify-content-end mt-2">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => setEditing(false)}
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          className="btn btn-sm btn-success"
                          disabled={savingEdit || !editTitle.trim()}
                        >
                          {savingEdit ? 'Saving...' : 'Save'}
                        </button>
                      </div>
                    </form>
                  )}
                </div>

                {/* Participants Section */}
                <div className="pt-3">
                  <div className="d-flex align-items-center justify-content-between mb-2">
                    <span className="fw-semibold small text-muted">
                      {group.members?.length || 0} PARTICIPANTS
                    </span>
                    {isAdmin && (
                      <button
                        className="btn btn-sm btn-outline-success py-0 px-2"
                        style={{ fontSize: '0.8rem' }}
                        onClick={handleToggleAddMembers}
                      >
                        <i className="bi bi-person-plus-fill me-1"></i>
                        {showAddMembers ? 'Cancel' : 'Add Members'}
                      </button>
                    )}
                  </div>

                  {/* Inline Add Members Panel */}
                  {showAddMembers && (
                    <div className="border rounded p-2 mb-3 bg-light">
                      <div className="fw-semibold small mb-1">Select Contacts to Add</div>
                      {loadingContacts ? (
                        <div className="text-center py-2">
                          <div className="spinner-border spinner-border-sm text-secondary" role="status"></div>
                        </div>
                      ) : contacts.length === 0 ? (
                        <div className="text-muted small py-2">No other contacts available to add.</div>
                      ) : (
                        <>
                          <div className="overflow-auto mb-2" style={{ maxHeight: '130px' }}>
                            {contacts.map((c) => {
                              const u = c.contactUser || c.user || c;
                              const isSel = selectedToAdd.some((m) => m.publicId === u.publicId);
                              return (
                                <div
                                  key={u.publicId}
                                  className={`d-flex align-items-center justify-content-between p-1 rounded ${
                                    isSel ? 'bg-white shadow-sm' : ''
                                  }`}
                                  style={{ cursor: 'pointer' }}
                                  onClick={() => toggleSelectToAdd(u)}
                                >
                                  <span className="small">{u.fullName || u.username}</span>
                                  <input
                                    type="checkbox"
                                    className="form-check-input"
                                    checked={isSel}
                                    onChange={() => {}}
                                  />
                                </div>
                              );
                            })}
                          </div>
                          <button
                            type="button"
                            className="btn btn-sm btn-success w-100"
                            disabled={selectedToAdd.length === 0 || addingMembers}
                            onClick={handleAddMembersSubmit}
                          >
                            {addingMembers ? 'Adding...' : `Add Selected (${selectedToAdd.length})`}
                          </button>
                        </>
                      )}
                    </div>
                  )}

                  {/* Members List */}
                  <div className="list-group list-group-flush">
                    {group.members?.map((m) => {
                      const isSelf = m.publicId === currentUser?.publicId;
                      const isMemberAdmin = m.role === 'ADMIN';
                      const isLoadingAction = actionLoadingId === m.publicId;

                      return (
                        <div
                          key={m.publicId}
                          className="list-group-item d-flex align-items-center justify-content-between px-0 py-2 border-bottom"
                        >
                          <div className="d-flex align-items-center gap-2">
                            <div
                              className="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold bg-secondary"
                              style={{ width: '36px', height: '36px', fontSize: '0.9rem' }}
                            >
                              {(m.fullName || m.username || 'U').charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <div className="fw-semibold small">
                                {m.fullName || m.username} {isSelf && <span className="text-muted">(You)</span>}
                              </div>
                              <div className="text-muted" style={{ fontSize: '0.72rem' }}>
                                @{m.username}
                              </div>
                            </div>
                          </div>

                          <div className="d-flex align-items-center gap-2">
                            {isMemberAdmin && (
                              <span className="badge bg-success-subtle text-success border border-success-subtle small px-2 py-1">
                                Group Admin
                              </span>
                            )}

                            {isAdmin && !isSelf && (
                              <div className="dropdown">
                                <button
                                  className="btn btn-sm btn-link text-secondary p-1"
                                  type="button"
                                  data-bs-toggle="dropdown"
                                  aria-expanded="false"
                                  disabled={isLoadingAction}
                                >
                                  {isLoadingAction ? (
                                    <span className="spinner-border spinner-border-sm" role="status"></span>
                                  ) : (
                                    <i className="bi bi-three-dots-vertical"></i>
                                  )}
                                </button>
                                <ul className="dropdown-menu dropdown-menu-end shadow-sm">
                                  <li>
                                    <button
                                      className="dropdown-item small"
                                      onClick={() => handleRoleToggle(m.publicId, m.role)}
                                    >
                                      {isMemberAdmin ? 'Dismiss as Admin' : 'Make Group Admin'}
                                    </button>
                                  </li>
                                  <li>
                                    <hr className="dropdown-margin my-1" />
                                  </li>
                                  <li>
                                    <button
                                      className="dropdown-item small text-danger"
                                      onClick={() => handleRemoveMember(m.publicId)}
                                    >
                                      Remove from Group
                                    </button>
                                  </li>
                                </ul>
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Leave Group Action */}
                <div className="pt-4 border-top mt-3 text-center">
                  {!confirmLeave ? (
                    <button
                      className="btn btn-link text-danger text-decoration-none small d-inline-flex align-items-center gap-1"
                      onClick={() => setConfirmLeave(true)}
                    >
                      <i className="bi bi-box-arrow-right"></i> Leave Group
                    </button>
                  ) : (
                    <div className="alert alert-warning p-2 small mb-0">
                      <p className="mb-2 fw-semibold">Are you sure you want to leave this group?</p>
                      <div className="d-flex justify-content-center gap-2">
                        <button
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => setConfirmLeave(false)}
                          disabled={leaving}
                        >
                          Cancel
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={handleLeaveGroup}
                          disabled={leaving}
                        >
                          {leaving ? 'Leaving...' : 'Yes, Leave'}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="text-center py-4 text-muted small">No group info available</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
