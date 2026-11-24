import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoginPage from '../LoginPage';
import { AuthContext } from '../../context/AuthContext';

describe('LoginPage', () => {
  const mockLogin = vi.fn();

  const renderComponent = (contextOverrides = {}) => {
    const defaultContext = {
      login: mockLogin,
      user: null,
      loading: false,
      isAuthenticated: false,
      ...contextOverrides
    };

    return render(
      <BrowserRouter>
        <AuthContext.Provider value={defaultContext}>
          <LoginPage />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders sign in form with inputs and submit button', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/username or email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('displays validation errors on empty submission', async () => {
    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/username or email is required/i)).toBeInTheDocument();
      expect(screen.getByText(/password is required/i)).toBeInTheDocument();
    });

    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('calls login on valid form submission', async () => {
    mockLogin.mockResolvedValueOnce({ success: true });
    renderComponent();

    fireEvent.change(screen.getByLabelText(/username or email/i), {
      target: { value: 'alice_j' }
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'Password123!' }
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        usernameOrEmail: 'alice_j',
        password: 'Password123!'
      });
    });
  });

  it('displays server error on login failure', async () => {
    mockLogin.mockRejectedValueOnce({
      message: 'Invalid username/email or password'
    });
    renderComponent();

    fireEvent.change(screen.getByLabelText(/username or email/i), {
      target: { value: 'alice_j' }
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'WrongPassword' }
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid username\/email or password/i)).toBeInTheDocument();
    });
  });
});
