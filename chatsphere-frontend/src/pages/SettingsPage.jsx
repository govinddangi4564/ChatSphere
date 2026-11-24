import React, { useState, useEffect } from 'react';
import Navbar from '../components/common/Navbar';
import { userService } from '../services/userService';

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState('privacy');

  // Privacy State
  const [privacy, setPrivacy] = useState({
    profilePhotoVisibility: 'EVERYONE',
    lastSeenVisibility: 'EVERYONE',
    onlineStatusVisibility: 'EVERYONE',
    messagePermission: 'EVERYONE'
  });
  const [loadingPrivacy, setLoadingPrivacy] = useState(true);
  const [savingPrivacy, setSavingPrivacy] = useState(false);
  const [privacySuccess, setPrivacySuccess] = useState(null);
  const [privacyError, setPrivacyError] = useState(null);

  // Blocked Users State
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [loadingBlocked, setLoadingBlocked] = useState(false);
  const [blockedError, setBlockedError] = useState(null);
  const [unblockingId, setUnblockingId] = useState(null);

  // Theme State
  const [isDarkMode, setIsDarkMode] = useState(() => {
    return localStorage.getItem('chatsphere_theme') === 'dark';
  });

  useEffect(() => {
    fetchPrivacy();
  }, []);

  useEffect(() => {
    if (activeTab === 'blocked') {
      fetchBlockedUsers();
    }
  }, [activeTab]);

  useEffect(() => {
    if (isDarkMode) {
      document.body.classList.add('dark-mode');
      localStorage.setItem('chatsphere_theme', 'dark');
    } else {
      document.body.classList.remove('dark-mode');
      localStorage.setItem('chatsphere_theme', 'light');
    }
  }, [isDarkMode]);

  const fetchPrivacy = async () => {
    try {
      setLoadingPrivacy(true);
      setPrivacyError(null);
      const data = await userService.getPrivacySettings();
      if (data) {
        setPrivacy(data);
      }
    } catch (err) {
      setPrivacyError(err.response?.data?.message || 'Failed to load privacy settings');
    } finally {
      setLoadingPrivacy(false);
    }
  };

  const handleSavePrivacy = async (e) => {
    e.preventDefault();
    try {
      setSavingPrivacy(true);
      setPrivacySuccess(null);
      setPrivacyError(null);
      const updated = await userService.updatePrivacySettings(privacy);
      setPrivacy(updated);
      setPrivacySuccess('Privacy settings saved successfully!');
    } catch (err) {
      setPrivacyError(err.response?.data?.message || 'Failed to save privacy settings');
    } finally {
      setSavingPrivacy(false);
    }
  };

  const fetchBlockedUsers = async () => {
    try {
      setLoadingBlocked(true);
      setBlockedError(null);
      const data = await userService.getBlockedUsers();
      setBlockedUsers(data || []);
    } catch (err) {
      setBlockedError(err.response?.data?.message || 'Failed to load blocked users');
    } finally {
      setLoadingBlocked(false);
    }
  };

  const handleUnblock = async (publicId) => {
    try {
      setUnblockingId(publicId);
      await userService.unblockUser(publicId);
      setBlockedUsers((prev) => prev.filter((u) => u.publicId !== publicId));
    } catch (err) {
      setBlockedError(err.response?.data?.message || 'Failed to unblock user');
    } finally {
      setUnblockingId(null);
    }
  };

  return (
    <div className="min-vh-100 d-flex flex-column">
      <Navbar />

      <main className="container py-4 flex-grow-1">
        <div className="row justify-content-center">
          <div className="col-12 col-md-9 col-lg-7">
            <div className="card card-custom p-4">
              <h3 className="fw-bold mb-4 text-center">Settings</h3>

              {/* Navigation Tabs */}
              <ul className="nav nav-tabs mb-4 justify-content-center">
                <li className="nav-item">
                  <button
                    className={`nav-link ${activeTab === 'privacy' ? 'active fw-bold' : ''}`}
                    onClick={() => setActiveTab('privacy')}
                  >
                    <i className="bi bi-shield-lock me-2"></i>Privacy
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    className={`nav-link ${activeTab === 'blocked' ? 'active fw-bold' : ''}`}
                    onClick={() => setActiveTab('blocked')}
                  >
                    <i className="bi bi-person-x me-2"></i>Blocked Users
                  </button>
                </li>
                <li className="nav-item">
                  <button
                    className={`nav-link ${activeTab === 'theme' ? 'active fw-bold' : ''}`}
                    onClick={() => setActiveTab('theme')}
                  >
                    <i className="bi bi-palette me-2"></i>Theme & Display
                  </button>
                </li>
              </ul>

              {/* Tab 1: Privacy Settings */}
              {activeTab === 'privacy' && (
                <div>
                  <h5 className="fw-semibold mb-3">Privacy & Visibility</h5>

                  {privacySuccess && (
                    <div className="alert alert-success alert-dismissible fade show" role="alert">
                      {privacySuccess}
                      <button type="button" className="btn-close" onClick={() => setPrivacySuccess(null)}></button>
                    </div>
                  )}

                  {privacyError && (
                    <div className="alert alert-danger alert-dismissible fade show" role="alert">
                      {privacyError}
                      <button type="button" className="btn-close" onClick={() => setPrivacyError(null)}></button>
                    </div>
                  )}

                  {loadingPrivacy ? (
                    <div className="text-center py-4">
                      <div className="spinner-border text-primary-color" role="status"></div>
                    </div>
                  ) : (
                    <form onSubmit={handleSavePrivacy}>
                      <div className="mb-3">
                        <label htmlFor="profilePhotoVisibility" className="form-label fw-semibold">
                          Who can see my profile photo
                        </label>
                        <select
                          id="profilePhotoVisibility"
                          className="form-select"
                          value={privacy.profilePhotoVisibility}
                          onChange={(e) =>
                            setPrivacy({ ...privacy, profilePhotoVisibility: e.target.value })
                          }
                        >
                          <option value="EVERYONE">Everyone</option>
                          <option value="CONTACTS_ONLY">My Contacts Only</option>
                          <option value="NOBODY">Nobody</option>
                        </select>
                      </div>

                      <div className="mb-3">
                        <label htmlFor="lastSeenVisibility" className="form-label fw-semibold">
                          Who can see my last seen
                        </label>
                        <select
                          id="lastSeenVisibility"
                          className="form-select"
                          value={privacy.lastSeenVisibility}
                          onChange={(e) =>
                            setPrivacy({ ...privacy, lastSeenVisibility: e.target.value })
                          }
                        >
                          <option value="EVERYONE">Everyone</option>
                          <option value="CONTACTS_ONLY">My Contacts Only</option>
                          <option value="NOBODY">Nobody</option>
                        </select>
                      </div>

                      <div className="mb-3">
                        <label htmlFor="onlineStatusVisibility" className="form-label fw-semibold">
                          Who can see when I am online
                        </label>
                        <select
                          id="onlineStatusVisibility"
                          className="form-select"
                          value={privacy.onlineStatusVisibility}
                          onChange={(e) =>
                            setPrivacy({ ...privacy, onlineStatusVisibility: e.target.value })
                          }
                        >
                          <option value="EVERYONE">Everyone</option>
                          <option value="CONTACTS_ONLY">My Contacts Only</option>
                          <option value="NOBODY">Nobody</option>
                        </select>
                      </div>

                      <div className="mb-4">
                        <label htmlFor="messagePermission" className="form-label fw-semibold">
                          Who can send me direct messages
                        </label>
                        <select
                          id="messagePermission"
                          className="form-select"
                          value={privacy.messagePermission}
                          onChange={(e) =>
                            setPrivacy({ ...privacy, messagePermission: e.target.value })
                          }
                        >
                          <option value="EVERYONE">Everyone</option>
                          <option value="CONTACTS_ONLY">My Contacts Only</option>
                        </select>
                      </div>

                      <button
                        type="submit"
                        className="btn btn-primary-custom w-100 py-2 fw-semibold"
                        disabled={savingPrivacy}
                      >
                        {savingPrivacy ? (
                          <>
                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                            Saving...
                          </>
                        ) : (
                          'Save Privacy Settings'
                        )}
                      </button>
                    </form>
                  )}
                </div>
              )}

              {/* Tab 2: Blocked Users */}
              {activeTab === 'blocked' && (
                <div>
                  <h5 className="fw-semibold mb-3">Blocked Users</h5>

                  {blockedError && (
                    <div className="alert alert-danger alert-dismissible fade show" role="alert">
                      {blockedError}
                      <button type="button" className="btn-close" onClick={() => setBlockedError(null)}></button>
                    </div>
                  )}

                  {loadingBlocked ? (
                    <div className="text-center py-4">
                      <div className="spinner-border text-primary-color" role="status"></div>
                    </div>
                  ) : blockedUsers.length === 0 ? (
                    <div className="text-center py-4 text-muted">
                      <i className="bi bi-shield-check fs-2 text-success mb-2 d-block"></i>
                      <p className="mb-0">You have not blocked any users.</p>
                    </div>
                  ) : (
                    <ul className="list-group list-group-flush">
                      {blockedUsers.map((user) => (
                        <li
                          key={user.publicId}
                          className="list-group-item d-flex justify-content-between align-items-center py-3 px-0"
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
                              <small className="text-muted">@{user.username}</small>
                            </div>
                          </div>

                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm px-3"
                            onClick={() => handleUnblock(user.publicId)}
                            disabled={unblockingId === user.publicId}
                          >
                            {unblockingId === user.publicId ? (
                              <span className="spinner-border spinner-border-sm" role="status"></span>
                            ) : (
                              'Unblock'
                            )}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              {/* Tab 3: Theme Settings */}
              {activeTab === 'theme' && (
                <div>
                  <h5 className="fw-semibold mb-3">Appearance & Theme</h5>
                  <p className="text-muted small">
                    Customize your visual experience in ChatSphere. Themes are saved locally in your browser.
                  </p>

                  <div className="d-flex justify-content-between align-items-center p-3 border rounded mb-3">
                    <div>
                      <h6 className="mb-1 fw-semibold">
                        <i className={`bi ${isDarkMode ? 'bi-moon-stars-fill' : 'bi-sun-fill'} me-2 text-warning`}></i>
                        {isDarkMode ? 'Dark Theme' : 'Light Theme'}
                      </h6>
                      <small className="text-muted">
                        {isDarkMode
                          ? 'Deep slate and dark tones for low-light environments'
                          : 'Clean light background with emerald & teal accents'}
                      </small>
                    </div>

                    <div className="form-check form-switch fs-4">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        role="switch"
                        id="themeSwitch"
                        checked={isDarkMode}
                        onChange={(e) => setIsDarkMode(e.target.checked)}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
