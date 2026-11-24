import React from 'react';

export default function ChatArea({
  conversation,
  onBack,
  onToggleArchive,
  onToggleMute,
  loadingAction
}) {
  if (!conversation) {
    return (
      <div className="chat-main align-items-center justify-content-center p-4 text-center">
        <div style={{ maxWidth: '420px' }}>
          <div className="mb-3">
            <span className="display-4 text-secondary-color">
              <i className="bi bi-chat-square-quote-fill"></i>
            </span>
          </div>
          <h4 className="fw-bold mb-2">ChatSphere Web</h4>
          <p className="text-muted small mb-0">
            Send and receive messages with your contacts. Select a conversation from the sidebar or click the new chat button to get started.
          </p>
        </div>
      </div>
    );
  }

  const partner = conversation.participants?.find((p) => p.role !== 'SELF'); // or simply first
  const displayTitle = conversation.title;
  const displayImage = conversation.imageUrl;

  return (
    <div className="chat-main">
      {/* Top Header */}
      <div className="chat-header justify-content-between shadow-sm">
        <div className="d-flex align-items-center gap-2 min-w-0">
          {/* Back button for mobile view */}
          <button
            className="btn btn-light btn-sm rounded-circle d-md-none me-1"
            onClick={onBack}
            title="Back to conversations"
          >
            <i className="bi bi-arrow-left"></i>
          </button>

          {displayImage ? (
            <img src={displayImage} alt={displayTitle} className="avatar-circle-sm" />
          ) : (
            <div className="avatar-circle-sm">
              {displayTitle ? displayTitle.charAt(0).toUpperCase() : 'C'}
            </div>
          )}

          <div className="min-w-0">
            <h6 className="mb-0 text-truncate fw-semibold">{displayTitle}</h6>
            <small className="text-muted d-block text-truncate" style={{ fontSize: '0.75rem' }}>
              {conversation.type === 'PRIVATE' ? 'End-to-end encrypted' : 'Group conversation'}
            </small>
          </div>
        </div>

        {/* Action icons */}
        <div className="d-flex align-items-center gap-2">
          <button
            className={`btn btn-sm ${conversation.muted ? 'btn-outline-warning' : 'btn-light'} rounded-circle`}
            onClick={() => onToggleMute(conversation.publicId, !conversation.muted)}
            disabled={loadingAction}
            title={conversation.muted ? 'Unmute' : 'Mute'}
          >
            <i className={`bi ${conversation.muted ? 'bi-bell-slash-fill' : 'bi-bell'}`}></i>
          </button>

          <button
            className={`btn btn-sm ${conversation.archived ? 'btn-outline-secondary' : 'btn-light'} rounded-circle`}
            onClick={() => onToggleArchive(conversation.publicId, !conversation.archived)}
            disabled={loadingAction}
            title={conversation.archived ? 'Unarchive' : 'Archive'}
          >
            <i className={`bi ${conversation.archived ? 'bi-archive-fill' : 'bi-archive'}`}></i>
          </button>
        </div>
      </div>

      {/* Messages Area (Phase 4 Placeholder) */}
      <div className="chat-messages d-flex flex-column justify-content-center align-items-center text-center">
        <div className="card card-custom p-4 border shadow-sm" style={{ maxWidth: '440px' }}>
          <div className="mb-2">
            <i className="bi bi-shield-lock-fill text-success fs-3"></i>
          </div>
          <h6 className="fw-bold mb-1">Beginning of Conversation</h6>
          <p className="text-muted small mb-3">
            This is the start of your private conversation with <strong>{displayTitle}</strong>.
          </p>
          <div className="p-2 bg-light rounded text-muted small">
            <i className="bi bi-info-circle me-1"></i>
            Real-time messaging, delivery receipts, and notifications are introduced in <strong>Phase 5</strong>.
          </div>
        </div>
      </div>

      {/* Bottom Message Input Bar */}
      <div className="chat-footer">
        <div className="input-group">
          <input
            type="text"
            className="form-control"
            placeholder="Messaging will be enabled in Phase 5..."
            disabled
          />
          <button className="btn btn-secondary" disabled>
            <i className="bi bi-send-fill"></i>
          </button>
        </div>
      </div>
    </div>
  );
}
