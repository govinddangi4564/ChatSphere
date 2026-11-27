import React from 'react';

export default function TypingIndicator({ username }) {
  return (
    <div className="d-flex align-items-center gap-2 mb-2 px-3 py-1">
      <div className="typing-bubble d-flex align-items-center gap-1 shadow-sm">
        <span className="typing-dot"></span>
        <span className="typing-dot"></span>
        <span className="typing-dot"></span>
      </div>
      {username && (
        <small className="text-muted" style={{ fontSize: '0.75rem' }}>
          {username} is typing...
        </small>
      )}
    </div>
  );
}
