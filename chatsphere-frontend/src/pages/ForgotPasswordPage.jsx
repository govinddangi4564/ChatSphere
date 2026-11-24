import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '../services/authService';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (!email.trim() || !/\S+@\S+\.\S+/.test(email)) {
      setError('Please provide a valid email address');
      return;
    }

    setSubmitting(true);
    try {
      const response = await authService.forgotPassword(email.trim());
      setMessage(
        response.message || 'If an account exists with this email, a reset link has been dispatched.'
      );
      setEmail('');
    } catch (err) {
      setError(err.message || 'Failed to dispatch reset link. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-4">
        <h4 className="fw-bold text-dark">Password Recovery</h4>
        <p className="text-muted small">Enter your email to receive a password reset link</p>
      </div>

      {error && (
        <div className="alert alert-danger py-2 small d-flex align-items-center gap-2" role="alert">
          <i className="bi bi-exclamation-circle-fill flex-shrink-0"></i>
          <div>{error}</div>
        </div>
      )}

      {message && (
        <div className="alert alert-info py-2 small" role="alert">
          <div className="d-flex align-items-center gap-2 mb-1">
            <i className="bi bi-info-circle-fill flex-shrink-0 fs-5"></i>
            <span className="fw-semibold">Instructions Dispatched</span>
          </div>
          <p className="mb-2 small">{message}</p>
          <Link to="/login" className="btn btn-sm btn-outline-primary w-100">
            Return to Sign In
          </Link>
        </div>
      )}

      {!message && (
        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="email">
              Registered Email Address
            </label>
            <div className="input-group">
              <span className="input-group-text bg-light text-muted">
                <i className="bi bi-envelope"></i>
              </span>
              <input
                type="email"
                id="email"
                name="email"
                className={`form-control ${error ? 'is-invalid' : ''}`}
                placeholder="e.g. john@example.com"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (error) setError('');
                }}
                disabled={submitting}
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary-custom w-100 py-2 mt-2 fw-medium d-flex align-items-center justify-content-center gap-2"
            disabled={submitting}
          >
            {submitting ? (
              <>
                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                Sending Link...
              </>
            ) : (
              'Send Reset Link'
            )}
          </button>
        </form>
      )}

      <div className="text-center mt-4 pt-2 border-top">
        <Link to="/login" className="small text-decoration-none text-secondary-color">
          <i className="bi bi-arrow-left me-1"></i>Back to Sign In
        </Link>
      </div>
    </div>
  );
}
