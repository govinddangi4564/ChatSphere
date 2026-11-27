import React, { useState } from 'react';

export default function ConversationList({
  conversations,
  selectedId,
  onSelectConversation,
  onOpenNewChatModal,
  onOpenNewGroupModal,
  activeFilter,
  onChangeFilter,
  loading
}) {
  const [filterQuery, setFilterQuery] = useState('');

  const filteredConversations = conversations.filter((c) => {
    if (!filterQuery.trim()) return true;
    const q = filterQuery.toLowerCase();
    const titleMatch = c.title && c.title.toLowerCase().includes(q);
    const usernameMatch = c.participant?.username && c.participant.username.toLowerCase().includes(q);
    return titleMatch || usernameMatch;
  });

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const now = new Date();
    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  };

  return (
    <div className="chat-sidebar">
      {/* Top Header */}
      <div className="p-3 border-bottom d-flex align-items-center justify-content-between">
        <h5 className="mb-0 fw-bold">Chats</h5>
        <div className="d-flex align-items-center gap-1">
          {onOpenNewGroupModal && (
            <button
              className="btn btn-light btn-sm rounded-circle d-flex align-items-center justify-content-center text-secondary border"
              style={{ width: '34px', height: '34px' }}
              onClick={onOpenNewGroupModal}
              title="New Group"
            >
              <i className="bi bi-people-fill"></i>
            </button>
          )}
          <button
            className="btn btn-primary-custom btn-sm rounded-circle d-flex align-items-center justify-content-center"
            style={{ width: '34px', height: '34px' }}
            onClick={onOpenNewChatModal}
            title="New Chat"
          >
            <i className="bi bi-chat-dots-fill"></i>
          </button>
        </div>
      </div>

      {/* Search Input */}
      <div className="p-2 border-bottom">
        <div className="input-group input-group-sm">
          <span className="input-group-text bg-light border-0">
            <i className="bi bi-search text-muted"></i>
          </span>
          <input
            type="text"
            className="form-control bg-light border-0"
            placeholder="Search or start new chat"
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="d-flex gap-1 p-2 border-bottom">
        <button
          className={`btn btn-sm rounded-pill px-3 py-1 ${activeFilter === 'all' ? 'btn-secondary text-white' : 'btn-light text-muted'}`}
          onClick={() => onChangeFilter('all')}
        >
          All
        </button>
        <button
          className={`btn btn-sm rounded-pill px-3 py-1 ${activeFilter === 'unread' ? 'btn-secondary text-white' : 'btn-light text-muted'}`}
          onClick={() => onChangeFilter('unread')}
        >
          Unread
        </button>
        <button
          className={`btn btn-sm rounded-pill px-3 py-1 ${activeFilter === 'archived' ? 'btn-secondary text-white' : 'btn-light text-muted'}`}
          onClick={() => onChangeFilter('archived')}
        >
          Archived
        </button>
      </div>

      {/* Conversation List */}
      <div className="flex-grow-1 overflow-auto">
        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border spinner-border-sm text-primary-color" role="status"></div>
          </div>
        ) : filteredConversations.length === 0 ? (
          <div className="text-center py-5 px-3 text-muted">
            <i className="bi bi-chat-left-dots fs-1 d-block mb-2 text-secondary"></i>
            <p className="mb-1 fw-semibold">No chats found</p>
            <small className="d-block mb-3">Start a conversation with any contact or user</small>
            <button className="btn btn-sm btn-primary-custom" onClick={onOpenNewChatModal}>
              Start a Chat
            </button>
          </div>
        ) : (
          <div>
            {filteredConversations.map((conv) => {
              const isSelected = conv.publicId === selectedId;
              const isGroup = conv.type === 'GROUP' || conv.conversationType === 'GROUP';

              return (
                <div
                  key={conv.publicId}
                  className={`conversation-item d-flex align-items-center gap-3 p-3 ${isSelected ? 'active' : ''}`}
                  onClick={() => onSelectConversation(conv.publicId)}
                >
                  {/* Avatar */}
                  {conv.imageUrl ? (
                    <img
                      src={conv.imageUrl}
                      alt={conv.title || 'Avatar'}
                      className="avatar-circle-sm flex-shrink-0"
                    />
                  ) : isGroup ? (
                    <div className="avatar-circle-sm flex-shrink-0 bg-success text-white">
                      <i className="bi bi-people-fill"></i>
                    </div>
                  ) : (
                    <div className="avatar-circle-sm flex-shrink-0">
                      {conv.title ? conv.title.charAt(0).toUpperCase() : 'C'}
                    </div>
                  )}

                  {/* Details */}
                  <div className="flex-grow-1 min-w-0">
                    <div className="d-flex justify-content-between align-items-baseline mb-1">
                      <h6 className="mb-0 text-truncate fw-semibold small">{conv.title}</h6>
                      <small className="text-muted" style={{ fontSize: '0.72rem' }}>
                        {formatTime(conv.lastMessageAt)}
                      </small>
                    </div>

                    <div className="d-flex justify-content-between align-items-center">
                      <small className="text-muted text-truncate" style={{ fontSize: '0.82rem' }}>
                        {conv.lastMessagePreview || (isGroup ? 'Group Chat' : 'Direct Message')}
                      </small>

                      <div className="d-flex align-items-center gap-1">
                        {conv.muted && <i className="bi bi-bell-slash text-muted" style={{ fontSize: '0.75rem' }}></i>}
                        {conv.archived && <i className="bi bi-archive text-muted" style={{ fontSize: '0.75rem' }}></i>}
                        {conv.unreadCount > 0 && (
                          <span className="unread-badge">{conv.unreadCount}</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
