import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function HomePage() {
  const { user, logout } = useAuth();

  return (
    <div className="home-wrapper min-vh-100 d-flex flex-column bg-light">
      {/* Top Navigation */}
      <nav className="navbar navbar-dark chatsphere-navbar shadow-sm">
        <div className="container">
          <Link to="/" className="navbar-brand d-flex align-items-center gap-2">
            <i className="bi bi-chat-dots-fill fs-4 text-accent"></i>
            <span className="brand-text">ChatSphere</span>
          </Link>
          <div className="d-flex align-items-center gap-3">
            <Link to="/health" className="btn btn-outline-light btn-sm d-none d-sm-inline-flex align-items-center gap-1">
              <i className="bi bi-activity"></i>
              System Health
            </Link>
            <button onClick={logout} className="btn btn-outline-light btn-sm d-flex align-items-center gap-1">
              <i className="bi bi-box-arrow-right"></i>
              Sign Out
            </button>
          </div>
        </div>
      </nav>

      {/* Main Container */}
      <div className="container flex-grow-1 py-5">
        <div className="row justify-content-center">
          <div className="col-12 col-md-10 col-lg-8">
            {/* Welcome Card */}
            <div className="card card-custom p-4 bg-white mb-4">
              <div className="d-flex flex-column flex-sm-row align-items-sm-center gap-3">
                <div className="avatar-circle">
                  {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="flex-grow-1">
                  <h4 className="fw-bold text-dark mb-1">
                    Welcome back, {user?.fullName || user?.username || 'User'}!
                  </h4>
                  <p className="text-muted small mb-0">
                    Logged in as <span className="fw-semibold text-secondary-color">@{user?.username}</span> &bull; {user?.email}
                  </p>
                </div>
                <span className="badge bg-success-subtle text-success px-3 py-2 rounded-pill align-self-start align-self-sm-center">
                  <i className="bi bi-check-circle-fill me-1"></i>
                  {user?.accountStatus || 'ACTIVE'}
                </span>
              </div>
            </div>

            {/* Application Overview & Next Phase Roadmap */}
            <div className="card card-custom p-4 bg-white">
              <h5 className="fw-bold text-dark mb-3">Your Account Details</h5>
              <div className="table-responsive">
                <table className="table table-borderless align-middle mb-0">
                  <tbody>
                    <tr>
                      <td className="text-muted small fw-medium" style={{ width: '180px' }}>
                        <i className="bi bi-person-badge me-2"></i>Public Identifier
                      </td>
                      <td className="font-monospace small text-dark">{user?.publicId || 'N/A'}</td>
                    </tr>
                    <tr>
                      <td className="text-muted small fw-medium">
                        <i className="bi bi-envelope-check me-2"></i>Email Verification
                      </td>
                      <td>
                        {user?.emailVerified ? (
                          <span className="badge bg-success-subtle text-success">Verified</span>
                        ) : (
                          <span className="badge bg-warning-subtle text-warning-emphasis">Pending Verification</span>
                        )}
                      </td>
                    </tr>
                    <tr>
                      <td className="text-muted small fw-medium">
                        <i className="bi bi-calendar-event me-2"></i>Account Created
                      </td>
                      <td className="small text-muted font-monospace">
                        {user?.createdAt ? new Date(user.createdAt).toLocaleString() : 'N/A'}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="alert alert-info py-3 mt-4 mb-0 d-flex align-items-center gap-3" role="alert">
                <i className="bi bi-info-circle-fill fs-3 text-secondary-color flex-shrink-0"></i>
                <div>
                  <h6 className="fw-bold mb-1">Phase 2B (Frontend Authentication) Complete</h6>
                  <p className="small mb-0 text-muted">
                    Your session is securely authenticated using short-lived JWT access tokens and HttpOnly refresh cookies.
                    Full messaging, conversations, and contact management will activate in subsequent phases.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
