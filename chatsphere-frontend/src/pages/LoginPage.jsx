import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function LoginPage() {
  const [formData, setFormData] = useState({ usernameOrEmail: '', password: '' });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const validate = () => {
    const errs = {};
    if (!formData.usernameOrEmail.trim()) {
      errs.usernameOrEmail = 'Username or email is required';
    }
    if (!formData.password) {
      errs.password = 'Password is required';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');

    if (!validate()) {
      return;
    }

    setSubmitting(true);
    try {
      await login(formData);
      navigate(from, { replace: true });
    } catch (err) {
      setServerError(err.message || 'Login failed. Please check your credentials.');
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
        <h4 className="fw-bold text-dark">Sign In</h4>
        <p className="text-muted small">Access your conversations and contacts</p>
      </div>

      {serverError && (
        <div className="alert alert-danger py-2 small d-flex align-items-center gap-2" role="alert">
          <i className="bi bi-exclamation-circle-fill flex-shrink-0"></i>
          <div>{serverError}</div>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="mb-3">
          <label className="form-label small fw-medium" htmlFor="usernameOrEmail">
            Username or Email
          </label>
          <div className="input-group">
            <span className="input-group-text bg-light text-muted">
              <i className="bi bi-person"></i>
            </span>
            <input
              type="text"
              id="usernameOrEmail"
              name="usernameOrEmail"
              className={`form-control ${errors.usernameOrEmail ? 'is-invalid' : ''}`}
              placeholder="e.g. johndoe or john@example.com"
              value={formData.usernameOrEmail}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.usernameOrEmail && (
              <div className="invalid-feedback">{errors.usernameOrEmail}</div>
            )}
          </div>
        </div>

        <div className="mb-3">
          <div className="d-flex justify-content-between align-items-center">
            <label className="form-label small fw-medium" htmlFor="password">
              Password
            </label>
            <Link to="/forgot-password" className="small text-decoration-none text-secondary-color">
              Forgot?
            </Link>
          </div>
          <div className="input-group">
            <span className="input-group-text bg-light text-muted">
              <i className="bi bi-lock"></i>
            </span>
            <input
              type="password"
              id="password"
              name="password"
              className={`form-control ${errors.password ? 'is-invalid' : ''}`}
              placeholder="Enter your password"
              value={formData.password}
              onChange={handleChange}
              disabled={submitting}
            />
            {errors.password && <div className="invalid-feedback">{errors.password}</div>}
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
              Signing In...
            </>
          ) : (
            'Sign In'
          )}
        </button>
      </form>

      <div className="text-center mt-4 pt-2 border-top">
        <p className="text-muted small mb-0">
          Don't have an account?{' '}
          <Link to="/register" className="fw-medium text-decoration-none text-secondary-color">
            Create account
          </Link>
        </p>
      </div>
    </div>
  );
}
