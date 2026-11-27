import React, { useState, useRef, useEffect } from 'react';
import { useNotifications } from '../../hooks/useNotifications';

export default function NotificationDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  const {
    notifications,
    unreadCount,
    loading,
    filter,
    setFilter,
    fetchNotifications,
    markAsRead,
    markAllAsRead
  } = useNotifications();

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggle = () => {
    const nextState = !isOpen;
    setIsOpen(nextState);
    if (nextState) {
      fetchNotifications();
    }
  };

  const getIconForType = (type) => {
    switch (type) {
      case 'NEW_MESSAGE':
        return <i className="bi bi-chat-text-fill text-primary"></i>;
      case 'GROUP_INVITE':
      case 'MEMBER_ADDED':
        return <i className="bi bi-people-fill text-success"></i>;
      case 'MEMBER_REMOVED':
        return <i className="bi bi-person-x-fill text-danger"></i>;
      case 'MENTION':
        return <i className="bi bi-at text-warning"></i>;
      default:
        return <i className="bi bi-bell-fill text-secondary"></i>;
    }
  };

  const formatTimestamp = (dateString) => {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now - date;
      const diffMins = Math.floor(diffMs / 60000);
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch {
      return '';
    }
  };

  const displayedNotifications = filter === 'unread'
    ? notifications.filter((n) => !n.isRead)
    : notifications;

  return (
    <div className="position-relative d-inline-block" ref={dropdownRef}>
      {/* Bell Button */}
      <button
        type="button"
        className="btn btn-link text-white position-relative p-2 text-decoration-none"
        onClick={handleToggle}
        aria-label="Notifications"
        title="Notifications"
      >
        <i className="bi bi-bell fs-5"></i>
        {unreadCount > 0 && (
          <span
            className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
            style={{ fontSize: '0.7rem' }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
            <span className="visually-hidden">unread notifications</span>
          </span>
        )}
      </button>

      {/* Dropdown Panel */}
      {isOpen && (
        <div
          className="card shadow-lg position-absolute end-0 mt-2 border-0"
          style={{
            width: '360px',
            maxWidth: '90vw',
            zIndex: 1050,
            borderRadius: '12px'
          }}
        >
          {/* Header */}
          <div className="card-header bg-white border-bottom d-flex align-items-center justify-content-between py-2 px-3">
            <div className="d-flex align-items-center gap-2">
              <span className="fw-bold fs-6">Notifications</span>
              {unreadCount > 0 && (
                <span className="badge bg-danger rounded-pill px-2">{unreadCount}</span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                className="btn btn-sm btn-link text-decoration-none text-success p-0 fw-semibold"
                onClick={markAllAsRead}
                style={{ fontSize: '0.8rem' }}
              >
                Mark all as read
              </button>
            )}
          </div>

          {/* Filter Tabs */}
          <div className="d-flex border-bottom bg-light px-3 py-1 gap-2">
            <button
              type="button"
              className={`btn btn-sm ${filter === 'all' ? 'btn-white fw-bold shadow-sm' : 'text-muted'} py-1 px-3 rounded-pill`}
              style={{ fontSize: '0.78rem' }}
              onClick={() => setFilter('all')}
            >
              All
            </button>
            <button
              type="button"
              className={`btn btn-sm ${filter === 'unread' ? 'btn-white fw-bold shadow-sm' : 'text-muted'} py-1 px-3 rounded-pill`}
              style={{ fontSize: '0.78rem' }}
              onClick={() => setFilter('unread')}
            >
              Unread
            </button>
          </div>

          {/* List */}
          <div
            className="card-body p-0"
            style={{ maxHeight: '380px', overflowY: 'auto' }}
          >
            {loading ? (
              <div className="p-4 text-center text-muted">
                <div className="spinner-border spinner-border-sm text-secondary mb-2" role="status"></div>
                <div className="small">Loading notifications...</div>
              </div>
            ) : displayedNotifications.length === 0 ? (
              <div className="p-4 text-center text-muted">
                <i className="bi bi-bell-slash fs-2 d-block mb-2 text-secondary"></i>
                <div className="small">No notifications to show</div>
              </div>
            ) : (
              <div className="list-group list-group-flush">
                {displayedNotifications.map((notif) => (
                  <div
                    key={notif.publicId}
                    className={`list-group-item list-group-item-action d-flex align-items-start gap-3 p-3 border-bottom ${
                      !notif.isRead ? 'bg-light' : ''
                    }`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => {
                      if (!notif.isRead) markAsRead(notif.publicId);
                    }}
                  >
                    <div className="fs-4 mt-1 flex-shrink-0">
                      {getIconForType(notif.notificationType)}
                    </div>
                    <div className="flex-grow-1 min-w-0">
                      <div className="d-flex align-items-baseline justify-content-between mb-1">
                        <span className={`small text-truncate ${!notif.isRead ? 'fw-bold text-dark' : 'text-secondary'}`}>
                          {notif.title}
                        </span>
                        <span className="text-muted" style={{ fontSize: '0.7rem' }}>
                          {formatTimestamp(notif.createdAt)}
                        </span>
                      </div>
                      <p className="mb-0 text-muted small text-break line-clamp-2" style={{ fontSize: '0.8rem' }}>
                        {notif.message}
                      </p>
                    </div>
                    {!notif.isRead && (
                      <span
                        className="rounded-circle bg-primary mt-2 flex-shrink-0"
                        style={{ width: '8px', height: '8px' }}
                        title="Unread"
                      ></span>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
