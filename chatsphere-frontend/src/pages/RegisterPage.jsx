import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function RegisterPage() {
  const [formData, setFormData] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { register } = useAuth();

  const validate = () => {
    const errs = {};
    if (!formData.fullName.trim() || formData.fullName.trim().length < 2) {
      errs.fullName = 'Full name must be at least 2 characters';
    }
    if (!formData.username.trim() || formData.username.trim().length < 3) {
      errs.username = 'Username must be at least 3 characters';
    } else if (!/^[a-zA-Z0-9_.]+$/.test(formData.username)) {
      errs.username = 'Username can only contain alphanumeric characters, dots, and underscores';
    }
    if (!formData.email.trim() || !/\S+@\S+\.\S+/.test(formData.email)) {
      errs.email = 'Please provide a valid email address';
    }
    if (!formData.password || formData.password.length < 8) {
      errs.password = 'Password must be at least 8 characters';
    }
    if (formData.password !== formData.confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setSuccessMessage('');

    if (!validate()) {
      return;
    }

    setSubmitting(true);
    try {
      const response = await register({
        fullName: formData.fullName,
        username: formData.username,
        email: formData.email,
        password: formData.password
      });

      setSuccessMessage(
        response.message || 'Registration successful! Please check your email to verify your account.'
      );
      setFormData({
        fullName: '',
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      });
    } catch (err) {
      setServerError(err.message || 'Registration failed. Please check the details and try again.');
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

  return (
    <div>
      <div className="text-center mb-4">
        <h4 className="fw-bold text-dark">Create Account</h4>
        <p className="text-muted small">Join ChatSphere to connect with everyone</p>
      </div>

      {serverError && (
        <div className="alert alert-danger py-2 small d-flex align-items-center gap-2" role="alert">
          <i className="bi bi-exclamation-circle-fill flex-shrink-0"></i>
          <div>{serverError}</div>
        </div>
      )}

      {successMessage && (
        <div className="alert alert-success py-2 small" role="alert">
          <div className="d-flex align-items-center gap-2 mb-1">
            <i className="bi bi-check-circle-fill flex-shrink-0 fs-5"></i>
            <span className="fw-semibold">Account Registered!</span>
          </div>
          <p className="mb-2 small">{successMessage}</p>
          <Link to="/login" className="btn btn-sm btn-outline-success w-100">
            Proceed to Sign In
          </Link>
        </div>
      )}

      {!successMessage && (
        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="fullName">
              Full Name
            </label>
            <input
              type="text"
              id="fullName"
              name="fullName"
              className={`form-control ${errors.fullName ? 'is-invalid' : ''}`}
              placeholder="e.g. Alice Johnson"
              value={formData.fullName}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.fullName && <div className="invalid-feedback">{errors.fullName}</div>}
          </div>

          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="username">
              Username
            </label>
            <input
              type="text"
              id="username"
              name="username"
              className={`form-control ${errors.username ? 'is-invalid' : ''}`}
              placeholder="e.g. alice_j"
              value={formData.username}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.username && <div className="invalid-feedback">{errors.username}</div>}
          </div>

          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="email">
              Email Address
            </label>
            <input
              type="email"
              id="email"
              name="email"
              className={`form-control ${errors.email ? 'is-invalid' : ''}`}
              placeholder="e.g. alice@example.com"
              value={formData.email}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.email && <div className="invalid-feedback">{errors.email}</div>}
          </div>

          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="password">
              Password
            </label>
            <input
              type="password"
              id="password"
              name="password"
              className={`form-control ${errors.password ? 'is-invalid' : ''}`}
              placeholder="At least 8 characters"
              value={formData.password}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.password && <div className="invalid-feedback">{errors.password}</div>}
          </div>

          <div className="mb-3">
            <label className="form-label small fw-medium" htmlFor="confirmPassword">
              Confirm Password
            </label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              className={`form-control ${errors.confirmPassword ? 'is-invalid' : ''}`}
              placeholder="Re-enter your password"
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
                Creating Account...
              </>
            ) : (
              'Create Account'
            )}
          </button>
        </form>
      )}

      <div className="text-center mt-4 pt-2 border-top">
        <p className="text-muted small mb-0">
          Already have an account?{' '}
          <Link to="/login" className="fw-medium text-decoration-none text-secondary-color">
            Sign In
          </Link>
        </p>
      </div>
    </div>
  );
}
