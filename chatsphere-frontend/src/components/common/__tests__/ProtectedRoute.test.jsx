import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import ProtectedRoute from '../ProtectedRoute';
import { AuthContext } from '../../../context/AuthContext';

describe('ProtectedRoute', () => {
  it('displays loading spinner while authentication status is resolving', () => {
    const mockContext = {
      isAuthenticated: false,
      loading: true,
      user: null
    };

    render(
      <AuthContext.Provider value={mockContext}>
        <MemoryRouter initialEntries={['/']}>
          <ProtectedRoute>
            <div>Protected Content</div>
          </ProtectedRoute>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.getByText(/validating authentication/i)).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('redirects unauthenticated users to /login', () => {
    const mockContext = {
      isAuthenticated: false,
      loading: false,
      user: null
    };

    render(
      <AuthContext.Provider value={mockContext}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <div>Protected Content</div>
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<div>Login Page Target</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.getByText('Login Page Target')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('renders protected child component when user is authenticated', () => {
    const mockContext = {
      isAuthenticated: true,
      loading: false,
      user: { username: 'alice', fullName: 'Alice' }
    };

    render(
      <AuthContext.Provider value={mockContext}>
        <MemoryRouter initialEntries={['/']}>
          <ProtectedRoute>
            <div>Protected Content Accessible</div>
          </ProtectedRoute>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.getByText('Protected Content Accessible')).toBeInTheDocument();
  });
});
