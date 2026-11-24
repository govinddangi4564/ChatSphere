import React, { useState, useEffect } from 'react';
import Navbar from '../components/common/Navbar';
import { contactService } from '../services/contactService';
import { userService } from '../services/userService';
import { useDebounce } from '../hooks/useDebounce';

export default function ContactsPage() {
  const [activeTab, setActiveTab] = useState('contacts');

  // Contacts List State
  const [contacts, setContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [contactsError, setContactsError] = useState(null);
  const [removingId, setRemovingId] = useState(null);

  // Search State
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedQuery = useDebounce(searchQuery, 350);
  const [searchResults, setSearchResults] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  useEffect(() => {
    fetchContacts();
  }, []);

  useEffect(() => {
    if (activeTab === 'search') {
      performSearch(debouncedQuery, page);
    }
  }, [debouncedQuery, page, activeTab]);

  const fetchContacts = async () => {
    try {
      setLoadingContacts(true);
      setContactsError(null);
      const data = await contactService.getContacts();
      setContacts(data || []);
    } catch (err) {
      setContactsError(err.response?.data?.message || 'Failed to load contacts');
    } finally {
      setLoadingContacts(false);
    }
  };

  const handleRemoveContact = async (publicId) => {
    if (!window.confirm('Are you sure you want to remove this contact?')) {
      return;
    }
    try {
      setRemovingId(publicId);
      await contactService.removeContact(publicId);
      setContacts((prev) => prev.filter((c) => c.publicId !== publicId));
    } catch (err) {
      setContactsError(err.response?.data?.message || 'Failed to remove contact');
    } finally {
      setRemovingId(null);
    }
  };

  const performSearch = async (query, pageNum) => {
    try {
      setSearching(true);
      setSearchError(null);
      const data = await userService.searchUsers(query, pageNum, 10, 'username,asc');
      if (data) {
        setSearchResults(data.content || []);
        setTotalPages(data.pagination?.totalPages || 0);
      }
    } catch (err) {
      setSearchError(err.response?.data?.message || 'Failed to search users');
    } finally {
      setSearching(false);
    }
  };

  const handleAddContact = async (publicId) => {
    try {
      setActionLoadingId(publicId);
      setActionSuccess(null);
      setSearchError(null);
      await contactService.addContact(publicId);
      setActionSuccess('Contact added successfully!');
      // Refresh contacts in background
      fetchContacts();
      // Update local search results
      setSearchResults((prev) =>
        prev.map((u) => (u.publicId === publicId ? { ...u, isContact: true } : u))
      );
    } catch (err) {
      setSearchError(err.response?.data?.message || 'Failed to add contact');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleBlockUser = async (publicId) => {
    if (!window.confirm('Block this user? They will not be able to message you or view your profile.')) {
      return;
    }
    try {
      setActionLoadingId(publicId);
      setActionSuccess(null);
      setSearchError(null);
      await userService.blockUser(publicId);
      setActionSuccess('User blocked successfully.');
      // Remove from contacts if present
      setContacts((prev) => prev.filter((c) => c.publicId !== publicId));
      // Remove from search results
      setSearchResults((prev) => prev.filter((u) => u.publicId !== publicId));
    } catch (err) {
      setSearchError(err.response?.data?.message || 'Failed to block user');
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="min-vh-100 d-flex flex-column">
      <Navbar />

      <main className="container py-4 flex-grow-1">
        <div className="row justify-content-center">
          <div className="col-12 col-md-9 col-lg-8">
            <div className="card card-custom p-4">
              <h3 className="fw-bold mb-4 text-center">Contacts & People</h3>

              {/* Navigation Tabs */}
              <ul className="nav nav-tabs mb-4 justify-content-center">
                <li className="nav-item">
                  <button
                    className={`nav-link ${activeTab === 'contacts' ? 'active fw-bold' : ''}`}
                    onClick={() => setActiveTab('contacts')}
                  >
                    <i className="bi bi-people-fill me-2"></i>My Contacts ({contacts.length})
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    className={`nav-link ${activeTab === 'search' ? 'active fw-bold' : ''}`}
                    onClick={() => {
                      setActiveTab('search');
                      setPage(0);
                    }}
                  >
                    <i className="bi bi-search me-2"></i>Find People
                  </button>
                </li>
              </ul>

              {/* Tab 1: Contacts List */}
              {activeTab === 'contacts' && (
                <div>
                  {contactsError && (
                    <div className="alert alert-danger alert-dismissible fade show" role="alert">
                      {contactsError}
                      <button type="button" className="btn-close" onClick={() => setContactsError(null)}></button>
                    </div>
                  )}

                  {loadingContacts ? (
                    <div className="text-center py-5">
                      <div className="spinner-border text-primary-color" role="status"></div>
                    </div>
                  ) : contacts.length === 0 ? (
                    <div className="text-center py-5 text-muted">
                      <i className="bi bi-person-lines-fill fs-1 text-secondary mb-2 d-block"></i>
                      <h5>No Contacts Yet</h5>
                      <p className="small mb-3">Search for users and add them to your contacts list.</p>
                      <button
                        className="btn btn-primary-custom btn-sm px-4"
                        onClick={() => setActiveTab('search')}
                      >
                        Find People
                      </button>
                    </div>
                  ) : (
                    <ul className="list-group list-group-flush">
                      {contacts.map((contact) => (
                        <li
                          key={contact.publicId}
                          className="list-group-item d-flex justify-content-between align-items-center py-3"
                        >
                          <div className="d-flex align-items-center gap-3">
                            {contact.profileImageUrl ? (
                              <img
                                src={contact.profileImageUrl}
                                alt={contact.fullName}
                                className="avatar-circle-sm"
                              />
                            ) : (
                              <div className="avatar-circle-sm">
                                {contact.fullName ? contact.fullName.charAt(0).toUpperCase() : 'U'}
                              </div>
                            )}
                            <div>
                              <h6 className="mb-0 fw-semibold">{contact.fullName}</h6>
                              <div className="text-muted small">@{contact.username}</div>
                              {contact.bio && <p className="mb-0 text-muted small mt-1">{contact.bio}</p>}
                            </div>
                          </div>

                          <div className="d-flex align-items-center gap-2">
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm"
                              onClick={() => handleRemoveContact(contact.publicId)}
                              disabled={removingId === contact.publicId}
                              title="Remove Contact"
                            >
                              {removingId === contact.publicId ? (
                                <span className="spinner-border spinner-border-sm" role="status"></span>
                              ) : (
                                <i className="bi bi-trash"></i>
                              )}
                            </button>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              {/* Tab 2: Find People */}
              {activeTab === 'search' && (
                <div>
                  <div className="mb-3">
                    <div className="input-group">
                      <span className="input-group-text bg-white">
                        <i className="bi bi-search text-muted"></i>
                      </span>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Search users by name or username..."
                        value={searchQuery}
                        onChange={(e) => {
                          setSearchQuery(e.target.value);
                          setPage(0);
                        }}
                      />
                    </div>
                  </div>

                  {actionSuccess && (
                    <div className="alert alert-success alert-dismissible fade show" role="alert">
                      {actionSuccess}
                      <button type="button" className="btn-close" onClick={() => setActionSuccess(null)}></button>
                    </div>
                  )}

                  {searchError && (
                    <div className="alert alert-danger alert-dismissible fade show" role="alert">
                      {searchError}
                      <button type="button" className="btn-close" onClick={() => setSearchError(null)}></button>
                    </div>
                  )}

                  {searching ? (
                    <div className="text-center py-4">
                      <div className="spinner-border text-primary-color" role="status"></div>
                    </div>
                  ) : searchResults.length === 0 ? (
                    <div className="text-center py-4 text-muted">
                      <p className="mb-0">No users found.</p>
                    </div>
                  ) : (
                    <div>
                      <ul className="list-group list-group-flush mb-3">
                        {searchResults.map((user) => (
                          <li
                            key={user.publicId}
                            className="list-group-item d-flex justify-content-between align-items-center py-3"
                          >
                            <div className="d-flex align-items-center gap-3">
                              {user.profileImageUrl ? (
                                <img
                                  src={user.profileImageUrl}
                                  alt={user.fullName}
                                  className="avatar-circle-sm"
                                />
                              ) : (
                                <div className="avatar-circle-sm">
                                  {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                                </div>
                              )}
                              <div>
                                <h6 className="mb-0 fw-semibold">{user.fullName}</h6>
                                <div className="text-muted small">@{user.username}</div>
                                {user.bio && <p className="mb-0 text-muted small mt-1">{user.bio}</p>}
                              </div>
                            </div>

                            <div className="d-flex align-items-center gap-2">
                              {user.isContact ? (
                                <span className="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                                  <i className="bi bi-check-circle me-1"></i>Contact
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  className="btn btn-outline-success btn-sm"
                                  onClick={() => handleAddContact(user.publicId)}
                                  disabled={actionLoadingId === user.publicId}
                                >
                                  {actionLoadingId === user.publicId ? (
                                    <span className="spinner-border spinner-border-sm" role="status"></span>
                                  ) : (
                                    <>
                                      <i className="bi bi-person-plus me-1"></i>Add
                                    </>
                                  )}
                                </button>
                              )}

                              <button
                                type="button"
                                className="btn btn-outline-secondary btn-sm"
                                onClick={() => handleBlockUser(user.publicId)}
                                disabled={actionLoadingId === user.publicId}
                                title="Block User"
                              >
                                <i className="bi bi-shield-slash"></i>
                              </button>
                            </div>
                          </li>
                        ))}
                      </ul>

                      {/* Pagination Controls */}
                      {totalPages > 1 && (
                        <div className="d-flex justify-content-between align-items-center pt-2">
                          <button
                            className="btn btn-outline-secondary btn-sm"
                            disabled={page === 0}
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                          >
                            Previous
                          </button>
                          <span className="small text-muted">
                            Page {page + 1} of {totalPages}
                          </span>
                          <button
                            className="btn btn-outline-secondary btn-sm"
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage((p) => p + 1)}
                          >
                            Next
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
