import React, { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authService } from '../services/authService';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [formData, setFormData] = useState({ newPassword: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    const errs = {};
    if (!formData.newPassword || formData.newPassword.length < 8) {
      errs.newPassword = 'New password must be at least 8 characters';
    }
    if (formData.newPassword !== formData.confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!token) {
      setServerError('Missing reset token. Please use the link sent to your email.');
      return;
    }

    if (!validate()) {
      return;
    }

    setSubmitting(true);
    try {
      await authService.resetPassword(token, formData.newPassword);
      setSuccess(true);
    } catch (err) {
      setServerError(err.message || 'Failed to reset password. The link may have expired.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  if (!token && !success) {
    return (
      <div className="text-center py-3">
        <i className="bi bi-exclamation-triangle text-warning fs-1"></i>
        <h5 className="mt-2 text-dark">Invalid Reset Link</h5>
        <p className="text-muted small">
          No password reset token was provided in the URL. Please check the link from your email.
        </p>
        <Link to="/forgot-password" className="btn btn-outline-secondary btn-sm mt-2">
          Request New Link
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="text-center mb-4">
        <h4 className="fw-bold text-dark">Reset Password</h4>
        <p className="text-muted small">Create a new secure password for your account</p>
      </div>

      {serverError && (
        <div className="alert alert-danger py-2 small d-flex align-items-center gap-2" role="alert">
          <i className="bi bi-exclamation-circle-fill flex-shrink-0"></i>
          <div>{serverError}</div>
        </div>
      )}

      {success ? (
        <div className="alert alert-success py-3 text-center" role="alert">
          <i className="bi bi-check-circle-fill fs-2 d-block mb-2 text-success"></i>
          <h6 className="fw-bold mb-1">Password Reset Successful</h6>
          <p className="small mb-3 text-muted">
            Your password has been updated. You can now log in with your new credentials.
          </p>
          <Link to="/login" className="btn btn-primary-custom btn-sm w-100">
            Sign In Now
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="newPassword">
              New Password
            </label>
            <input
              type="password"
              id="newPassword"
              name="newPassword"
              className={`form-control ${errors.newPassword ? 'is-invalid' : ''}`}
              placeholder="At least 8 characters"
              value={formData.newPassword}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.newPassword && <div className="invalid-feedback">{errors.newPassword}</div>}
          </div>

          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="confirmPassword">
              Confirm New Password
            </label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              className={`form-control ${errors.confirmPassword ? 'is-invalid' : ''}`}
              placeholder="Re-enter your new password"
              value={formData.confirmPassword}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.confirmPassword && (
              <div className="invalid-feedback">{errors.confirmPassword}</div>
            )}
          </div>

          <button
            type="submit"
            className="btn btn-primary-custom w-100 py-2 mt-2 fw-medium d-flex align-items-center justify-content-center gap-2"
            disabled={submitting}
          >
            {submitting ? (
              <>
                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                Updating Password...
              </>
            ) : (
              'Reset Password'
            )}
          </button>
        </form>
      )}
    </div>
  );
}
