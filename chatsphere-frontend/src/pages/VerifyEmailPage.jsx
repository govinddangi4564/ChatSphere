import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authService } from '../services/authService';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [loading, setLoading] = useState(Boolean(token));
  const [status, setStatus] = useState(token ? 'verifying' : 'missing');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('missing');
      return;
    }

    let isMounted = true;

    const verify = async () => {
      try {
        const response = await authService.verifyEmail(token);
        if (isMounted) {
          setStatus('success');
          setMessage(response.message || 'Your email has been verified successfully!');
        }
      } catch (err) {
        if (isMounted) {
          setStatus('error');
          setMessage(err.message || 'Verification token is invalid or has expired.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    verify();

    return () => {
      isMounted = false;
    };
  }, [token]);

  return (
    <div className="text-center py-2">
      {loading && (
        <div className="py-4">
          <div className="spinner-border text-primary-color mb-3" role="status">
            <span className="visually-hidden">Verifying...</span>
          </div>
          <h5 className="fw-bold text-dark">Verifying Email</h5>
          <p className="text-muted small">Please wait while we confirm your account...</p>
        </div>
      )}

      {status === 'success' && !loading && (
        <div>
          <i className="bi bi-patch-check-fill text-success fs-1 d-block mb-2"></i>
          <h5 className="fw-bold text-dark">Email Verified!</h5>
          <p className="text-muted small mb-4">{message}</p>
          <Link to="/login" className="btn btn-primary-custom w-100 py-2">
            Proceed to Sign In
          </Link>
        </div>
      )}

      {status === 'error' && !loading && (
        <div>
          <i className="bi bi-x-circle-fill text-danger fs-1 d-block mb-2"></i>
          <h5 className="fw-bold text-dark">Verification Failed</h5>
          <p className="text-muted small mb-4">{message}</p>
          <Link to="/login" className="btn btn-outline-secondary w-100 py-2">
            Back to Sign In
          </Link>
        </div>
      )}

      {status === 'missing' && (
        <div>
          <i className="bi bi-envelope-exclamation text-warning fs-1 d-block mb-2"></i>
          <h5 className="fw-bold text-dark">Verification Link Required</h5>
          <p className="text-muted small mb-4">
            No verification token was provided. Please click the link received in your registration email.
          </p>
          <Link to="/login" className="btn btn-outline-secondary w-100 py-2">
            Return to Sign In
          </Link>
        </div>
      )}
    </div>
  );
}
