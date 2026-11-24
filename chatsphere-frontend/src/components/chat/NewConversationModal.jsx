import React, { useState, useEffect } from 'react';
import { userService } from '../../services/userService';
import { contactService } from '../../services/contactService';
import { conversationService } from '../../services/conversationService';
import { useDebounce } from '../../hooks/useDebounce';

export default function NewConversationModal({ show, onClose, onConversationSelected }) {
  const [activeTab, setActiveTab] = useState('contacts');
  const [contacts, setContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(false);

  const [searchQuery, setSearchQuery] = useState('');
  const debouncedQuery = useDebounce(searchQuery, 350);
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);

  const [loadingAction, setLoadingAction] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (show) {
      setError(null);
      fetchContacts();
    }
  }, [show]);

  useEffect(() => {
    if (show && activeTab === 'search') {
      searchUsers(debouncedQuery);
    }
  }, [debouncedQuery, activeTab, show]);

  const fetchContacts = async () => {
    try {
      setLoadingContacts(true);
      const data = await contactService.getContacts();
      setContacts(data || []);
    } catch {
      // ignore
    } finally {
      setLoadingContacts(false);
    }
  };

  const searchUsers = async (query) => {
    try {
      setSearching(true);
      const data = await userService.searchUsers(query, 0, 15);
      setSearchResults(data?.content || []);
    } catch {
      // ignore
    } finally {
      setSearching(false);
    }
  };

  const handleStartChat = async (recipientPublicId) => {
    try {
      setLoadingAction(true);
      setError(null);
      const conversation = await conversationService.createOrFindPrivateConversation(recipientPublicId);
      onConversationSelected(conversation);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to start conversation');
    } finally {
      setLoadingAction(false);
    }
  };

  if (!show) return null;

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content card-custom">
          <div className="modal-header">
            <h5 className="modal-title fw-bold">
              <i className="bi bi-chat-plus me-2 text-primary-color"></i>New Conversation
            </h5>
            <button type="button" className="btn-close" onClick={onClose} disabled={loadingAction}></button>
          </div>

          <div className="modal-body p-4">
            {error && (
              <div className="alert alert-danger alert-dismissible fade show mb-3" role="alert">
                {error}
                <button type="button" className="btn-close" onClick={() => setError(null)}></button>
              </div>
            )}

            {/* Tabs */}
            <ul className="nav nav-pills nav-fill mb-3">
              <li className="nav-item">
                <button
                  className={`nav-link py-1 ${activeTab === 'contacts' ? 'active bg-primary-color' : ''}`}
                  onClick={() => setActiveTab('contacts')}
                >
                  My Contacts
                </button>
              </li>
              <li className="nav-item">
                <button
                  className={`nav-link py-1 ${activeTab === 'search' ? 'active bg-primary-color' : ''}`}
                  onClick={() => setActiveTab('search')}
                >
                  Find People
                </button>
              </li>
            </ul>

            {activeTab === 'contacts' && (
              <div>
                {loadingContacts ? (
                  <div className="text-center py-4">
                    <div className="spinner-border spinner-border-sm text-primary-color" role="status"></div>
                  </div>
                ) : contacts.length === 0 ? (
                  <div className="text-center py-4 text-muted small">
                    <p className="mb-2">No contacts saved yet.</p>
                    <button
                      className="btn btn-sm btn-outline-secondary"
                      onClick={() => setActiveTab('search')}
                    >
                      Search for people
                    </button>
                  </div>
                ) : (
                  <ul className="list-group list-group-flush">
                    {contacts.map((contact) => (
                      <li
                        key={contact.publicId}
                        className="list-group-item list-group-item-action d-flex align-items-center justify-content-between p-2 rounded cursor-pointer"
                        onClick={() => handleStartChat(contact.publicId)}
                      >
                        <div className="d-flex align-items-center gap-2">
                          {contact.profileImageUrl ? (
                            <img src={contact.profileImageUrl} alt={contact.fullName} className="avatar-circle-sm" />
                          ) : (
                            <div className="avatar-circle-sm">
                              {contact.fullName ? contact.fullName.charAt(0).toUpperCase() : 'U'}
                            </div>
                          )}
                          <div>
                            <div className="fw-semibold small">{contact.fullName}</div>
                            <div className="text-muted small">@{contact.username}</div>
                          </div>
                        </div>
                        <i className="bi bi-chevron-right text-muted small"></i>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {activeTab === 'search' && (
              <div>
                <div className="input-group mb-3">
                  <span className="input-group-text bg-white">
                    <i className="bi bi-search text-muted"></i>
                  </span>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Search users..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    autoFocus
                  />
                </div>

                {searching ? (
                  <div className="text-center py-4">
                    <div className="spinner-border spinner-border-sm text-primary-color" role="status"></div>
                  </div>
                ) : searchResults.length === 0 ? (
                  <div className="text-center py-4 text-muted small">
                    {searchQuery ? 'No users found matching query' : 'Type a name or username to search'}
                  </div>
                ) : (
                  <ul className="list-group list-group-flush">
                    {searchResults.map((user) => (
                      <li
                        key={user.publicId}
                        className="list-group-item list-group-item-action d-flex align-items-center justify-content-between p-2 rounded cursor-pointer"
                        onClick={() => handleStartChat(user.publicId)}
                      >
                        <div className="d-flex align-items-center gap-2">
                          {user.profileImageUrl ? (
                            <img src={user.profileImageUrl} alt={user.fullName} className="avatar-circle-sm" />
                          ) : (
                            <div className="avatar-circle-sm">
                              {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                            </div>
                          )}
                          <div>
                            <div className="fw-semibold small">{user.fullName}</div>
                            <div className="text-muted small">@{user.username}</div>
                          </div>
                        </div>
                        <button className="btn btn-sm btn-outline-primary rounded-pill px-3">
                          Chat
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
