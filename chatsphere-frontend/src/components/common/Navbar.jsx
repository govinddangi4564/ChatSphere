import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import NotificationDropdown from '../notification/NotificationDropdown';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar navbar-expand-lg navbar-dark chatsphere-navbar shadow-sm">
      <div className="container">
        <Link to="/" className="navbar-brand d-flex align-items-center gap-2 text-decoration-none">
          <i className="bi bi-chat-dots-fill fs-4 text-warning"></i>
          <span className="brand-text">ChatSphere</span>
        </Link>

        {isAuthenticated && (
          <div className="d-flex align-items-center gap-2 order-lg-last">
            <NotificationDropdown />

            <Link
              to="/profile"
              className="d-flex align-items-center gap-2 text-decoration-none text-white me-2"
              title="View Profile"
            >
              {user?.profileImageUrl ? (
                <img
                  src={user.profileImageUrl}
                  alt={user.fullName || 'User'}
                  className="avatar-circle-sm border border-light"
                />
              ) : (
                <div className="avatar-circle-sm border border-light bg-light text-dark">
                  {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                </div>
              )}
              <span className="d-none d-md-inline fw-semibold small">
                {user?.fullName || user?.username}
              </span>
            </Link>

            <button
              onClick={handleLogout}
              className="btn btn-outline-light btn-sm rounded-pill px-3"
            >
              <i className="bi bi-box-arrow-right me-1"></i>Logout
            </button>
          </div>
        )}

        <button
          className="navbar-toggler border-0"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#chatsphereNavbarContent"
          aria-controls="chatsphereNavbarContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        {isAuthenticated && (
          <div className="collapse navbar-collapse" id="chatsphereNavbarContent">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0">
              <li className="nav-item">
                <Link
                  to="/"
                  className={`nav-link ${isActive('/') ? 'active fw-bold' : ''}`}
                >
                  <i className="bi bi-chat-text me-1"></i>Chats
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  to="/contacts"
                  className={`nav-link ${isActive('/contacts') ? 'active fw-bold' : ''}`}
                >
                  <i className="bi bi-people me-1"></i>Contacts
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  to="/profile"
                  className={`nav-link ${isActive('/profile') ? 'active fw-bold' : ''}`}
                >
                  <i className="bi bi-person me-1"></i>Profile
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  to="/settings"
                  className={`nav-link ${isActive('/settings') ? 'active fw-bold' : ''}`}
                >
                  <i className="bi bi-gear me-1"></i>Settings
                </Link>
              </li>
            </ul>
          </div>
        )}
      </div>
    </nav>
  );
}
