import React, { useState, useEffect } from 'react';
import Navbar from '../components/common/Navbar';
import { userService } from '../services/userService';

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [fullName, setFullName] = useState('');
  const [bio, setBio] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState('');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getMe();
      setProfile(data);
      if (data) {
        setFullName(data.fullName || '');
        setBio(data.bio || '');
        setProfileImageUrl(data.profileImageUrl || '');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setError('Full name cannot be empty');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setSuccess(null);
      const updated = await userService.updateProfile({
        fullName: fullName.trim(),
        bio: bio.trim(),
        profileImageUrl: profileImageUrl.trim()
      });
      setProfile(updated);
      setSuccess('Profile updated successfully!');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-vh-100 d-flex flex-column">
      <Navbar />

      <main className="container py-4 flex-grow-1">
        <div className="row justify-content-center">
          <div className="col-12 col-md-8 col-lg-6">
            <div className="card card-custom p-4">
              <h3 className="fw-bold mb-4 text-center">My Profile</h3>

              {loading ? (
                <div className="text-center py-5">
                  <div className="spinner-border text-primary-color" role="status">
                    <span className="visually-hidden">Loading profile...</span>
                  </div>
                </div>
              ) : (
                <>
                  {error && (
                    <div className="alert alert-danger alert-dismissible fade show" role="alert">
                      {error}
                      <button type="button" className="btn-close" onClick={() => setError(null)}></button>
                    </div>
                  )}

                  {success && (
                    <div className="alert alert-success alert-dismissible fade show" role="alert">
                      {success}
                      <button type="button" className="btn-close" onClick={() => setSuccess(null)}></button>
                    </div>
                  )}

                  {/* Profile Avatar & Preview */}
                  <div className="d-flex flex-column align-items-center mb-4">
                    {profileImageUrl ? (
                      <img
                        src={profileImageUrl}
                        alt={fullName || 'Avatar'}
                        className="avatar-circle-lg shadow-sm mb-2"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = 'https://via.placeholder.com/96?text=User';
                        }}
                      />
                    ) : (
                      <div className="avatar-circle-lg shadow-sm mb-2">
                        {fullName ? fullName.charAt(0).toUpperCase() : 'U'}
                      </div>
                    )}
                    <h5 className="fw-bold mb-1">{profile?.fullName}</h5>
                    <span className="text-muted">@{profile?.username}</span>
                    <div className="mt-2 d-flex gap-2">
                      <span className="badge bg-secondary">
                        {profile?.emailVerified ? 'Verified Email' : 'Unverified'}
                      </span>
                      <span className="badge bg-info text-dark">
                        Status: {profile?.accountStatus}
                      </span>
                    </div>
                  </div>

                  {/* Profile Edit Form */}
                  <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                      <label htmlFor="fullName" className="form-label fw-semibold">
                        Full Name
                      </label>
                      <input
                        id="fullName"
                        type="text"
                        className="form-control"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        required
                        maxLength={100}
                      />
                    </div>

                    <div className="mb-3">
                      <label htmlFor="bio" className="form-label fw-semibold">
                        Bio / About
                      </label>
                      <textarea
                        id="bio"
                        className="form-control"
                        rows="3"
                        placeholder="Tell contacts a little about yourself"
                        value={bio}
                        onChange={(e) => setBio(e.target.value)}
                        maxLength={255}
                      />
                    </div>

                    <div className="mb-3">
                      <label htmlFor="profileImageUrl" className="form-label fw-semibold">
                        Profile Image URL
                      </label>
                      <input
                        id="profileImageUrl"
                        type="url"
                        className="form-control"
                        placeholder="https://example.com/photo.jpg"
                        value={profileImageUrl}
                        onChange={(e) => setProfileImageUrl(e.target.value)}
                        maxLength={255}
                      />
                      <div className="form-text mt-1 text-muted">
                        <i className="bi bi-info-circle me-1"></i>
                        Accepts an image URL for now. Direct file upload to cloud storage arrives in Phase 7.
                      </div>
                    </div>

                    <div className="mb-3">
                      <label className="form-label fw-semibold text-muted">
                        Email Address (Read-Only)
                      </label>
                      <input
                        type="email"
                        className="form-control bg-light"
                        value={profile?.email || ''}
                        disabled
                      />
                    </div>

                    <div className="mb-4">
                      <label className="form-label fw-semibold text-muted">
                        Public User ID
                      </label>
                      <input
                        type="text"
                        className="form-control bg-light font-monospace small"
                        value={profile?.publicId || ''}
                        disabled
                      />
                    </div>

                    <button
                      type="submit"
                      className="btn btn-primary-custom w-100 py-2 fw-semibold"
                      disabled={saving}
                    >
                      {saving ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                          Saving Changes...
                        </>
                      ) : (
                        'Save Profile'
                      )}
                    </button>
                  </form>
                </>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
