import React, { useState, useEffect } from 'react';
import { healthService } from '../services/healthService';

export default function HealthCheckPage() {
  const [healthData, setHealthData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  const fetchHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await healthService.checkHealth();
      setHealthData(result.data);
      setLastChecked(new Date().toLocaleTimeString());
    } catch (err) {
      setError(err.message || 'Unable to connect to ChatSphere API');
      setLastChecked(new Date().toLocaleTimeString());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="card card-custom p-4 bg-white">
            <div className="d-flex justify-content-between align-items-center mb-4">
              <div>
                <h4 className="card-title mb-1 fw-bold text-dark">System Health</h4>
                <p className="text-muted small mb-0">Phase 1 Foundation Health Monitor</p>
              </div>
              <button
                className="btn btn-outline-success btn-sm d-flex align-items-center gap-2"
                onClick={fetchHealth}
                disabled={loading}
              >
                <i className={`bi bi-arrow-clockwise ${loading ? 'spin' : ''}`}></i>
                Refresh
              </button>
            </div>

            {loading && (
              <div className="text-center py-4">
                <div className="spinner-border text-success" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
                <p className="text-muted small mt-2 mb-0">Querying backend /api/health...</p>
              </div>
            )}

            {error && !loading && (
              <div className="alert alert-danger d-flex align-items-start gap-3" role="alert">
                <i className="bi bi-exclamation-triangle-fill fs-4 flex-shrink-0"></i>
                <div>
                  <h6 className="alert-heading mb-1 fw-bold">Connection Degraded</h6>
                  <p className="mb-1 small">{error}</p>
                  <small className="text-muted">Ensure backend is running at http://localhost:8080</small>
                </div>
              </div>
            )}

            {healthData && !loading && (
              <div className="list-group list-group-flush border rounded-3 overflow-hidden">
                <div className="list-group-item d-flex justify-content-between align-items-center p-3">
                  <span className="fw-medium text-secondary">
                    <i className="bi bi-cpu me-2"></i>Backend Service
                  </span>
                  <span className={`badge status-badge ${healthData.status === 'UP' ? 'bg-success' : 'bg-warning text-dark'}`}>
                    {healthData.status}
                  </span>
                </div>
                <div className="list-group-item d-flex justify-content-between align-items-center p-3">
                  <span className="fw-medium text-secondary">
                    <i className="bi bi-database me-2"></i>Database (MySQL)
                  </span>
                  <span className={`badge status-badge ${healthData.database === 'UP' ? 'bg-success' : 'bg-danger'}`}>
                    {healthData.database}
                  </span>
                </div>
                <div className="list-group-item d-flex justify-content-between align-items-center p-3">
                  <span className="fw-medium text-secondary">
                    <i className="bi bi-tag me-2"></i>App Version
                  </span>
                  <span className="font-monospace text-dark">{healthData.version}</span>
                </div>
                <div className="list-group-item d-flex justify-content-between align-items-center p-3">
                  <span className="fw-medium text-secondary">
                    <i className="bi bi-clock me-2"></i>Server Timestamp
                  </span>
                  <span className="small text-muted font-monospace">
                    {new Date(healthData.timestamp).toISOString()}
                  </span>
                </div>
              </div>
            )}

            {lastChecked && (
              <div className="text-center mt-4">
                <small className="text-muted">Last checked at: {lastChecked}</small>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
