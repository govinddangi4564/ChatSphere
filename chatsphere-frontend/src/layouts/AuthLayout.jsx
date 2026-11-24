import React from 'react';
import { Outlet, Link } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <div className="auth-wrapper min-vh-100 d-flex flex-column">
      {/* Top Brand Banner */}
      <div className="auth-header py-4 text-center">
        <Link to="/" className="text-decoration-none d-inline-flex align-items-center gap-2">
          <i className="bi bi-chat-dots-fill fs-1 text-accent"></i>
          <span className="brand-text text-white fs-2 fw-bold">ChatSphere</span>
        </Link>
        <p className="text-white-50 small mb-0 mt-1">Connect. Communicate. Collaborate.</p>
      </div>

      {/* Main Form Container */}
      <div className="container flex-grow-1 d-flex align-items-center justify-content-center py-4">
        <div className="row justify-content-center w-100">
          <div className="col-12 col-sm-10 col-md-8 col-lg-5 col-xl-4">
            <div className="card card-custom p-4 bg-white">
              <Outlet />
            </div>
          </div>
        </div>
      </div>

      {/* Footer */}
      <footer className="py-3 text-center text-muted small">
        <div className="container">
          <p className="mb-0">&copy; {new Date().getFullYear()} ChatSphere. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
}
