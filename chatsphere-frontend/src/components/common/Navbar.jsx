import React from 'react';

export default function Navbar() {
  return (
    <nav className="navbar navbar-dark chatsphere-navbar shadow-sm">
      <div className="container">
        <span className="navbar-brand d-flex align-items-center gap-2">
          <i className="bi bi-chat-dots-fill fs-4 text-warning"></i>
          <span className="brand-text">ChatSphere</span>
        </span>
        <span className="badge bg-light text-dark px-3 py-2 rounded-pill font-monospace">
          v1.0.0 (Phase 1 Foundation)
        </span>
      </div>
    </nav>
  );
}
