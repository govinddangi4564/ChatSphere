import React from 'react';

export default function MessageBubble({ message, isOwnMessage, isOutgoing, isGroup, onRetry }) {
  const isMine = isOwnMessage !== undefined ? isOwnMessage : Boolean(isOutgoing);

  const formatTime = (isoString) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  // System message notice
  if (message.messageType === 'SYSTEM') {
    return (
      <div className="text-center my-2">
        <span
          className="badge rounded-pill bg-light text-muted border px-3 py-1 shadow-sm"
          style={{ fontSize: '0.75rem', fontWeight: 500 }}
        >
          {message.content}
        </span>
      </div>
    );
  }

  const renderStatus = () => {
    if (!isMine) return null;

    switch (message.status) {
      case 'SENDING':
        return (
          <i
            className="bi bi-clock text-muted ms-1"
            title="Sending..."
            style={{ fontSize: '0.72rem' }}
          ></i>
        );
      case 'FAILED':
        return (
          <button
            className="btn btn-link btn-sm p-0 text-danger ms-1 text-decoration-none d-inline-flex align-items-center gap-1"
            onClick={() => onRetry && onRetry(message.clientMessageId)}
            title="Failed to send. Click to retry."
            style={{ fontSize: '0.72rem' }}
          >
            <i className="bi bi-exclamation-circle-fill"></i> Retry
          </button>
        );
      case 'READ':
        return (
          <i
            className="bi bi-check-all ms-1 text-info"
            title="Read"
            style={{ fontSize: '0.85rem', color: '#53bdeb' }}
          ></i>
        );
      case 'DELIVERED':
        return (
          <i
            className="bi bi-check-all ms-1 text-muted"
            title="Delivered"
            style={{ fontSize: '0.85rem' }}
          ></i>
        );
      case 'SENT':
      default:
        return (
          <i
            className="bi bi-check ms-1 text-muted"
            title="Sent"
            style={{ fontSize: '0.85rem' }}
          ></i>
        );
    }
  };

  const senderDisplayName = message.senderFullName || message.senderUsername;

  return (
    <div className={`d-flex ${isMine ? 'justify-content-end' : 'justify-content-start'} mb-2`}>
      <div className={`message-bubble ${isMine ? 'message-bubble-outgoing' : 'message-bubble-incoming'} shadow-sm`}>
        {isGroup && !isMine && senderDisplayName && (
          <div className="fw-bold mb-1" style={{ color: '#128C7E', fontSize: '0.78rem' }}>
            {senderDisplayName}
          </div>
        )}
        <div className="message-content">{message.content}</div>
        <div className="message-meta d-flex align-items-center justify-content-end gap-1 mt-1">
          <span className="message-time">{formatTime(message.createdAt)}</span>
          {renderStatus()}
        </div>
      </div>
    </div>
  );
}
