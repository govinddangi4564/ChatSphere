import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import RegisterPage from '../RegisterPage';
import { AuthContext } from '../../context/AuthContext';

describe('RegisterPage', () => {
  const mockRegister = vi.fn();

  const renderComponent = () => {
    const defaultContext = {
      register: mockRegister,
      user: null,
      loading: false,
      isAuthenticated: false
    };

    return render(
      <BrowserRouter>
        <AuthContext.Provider value={defaultContext}>
          <RegisterPage />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders registration form fields', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/^full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^confirm password/i)).toBeInTheDocument();
  });

  it('validates password length and matching passwords', async () => {
    renderComponent();

    fireEvent.change(screen.getByLabelText(/^full name/i), {
      target: { value: 'Bob Smith' }
    });
    fireEvent.change(screen.getByLabelText(/^username/i), {
      target: { value: 'bob_smith' }
    });
    fireEvent.change(screen.getByLabelText(/^email address/i), {
      target: { value: 'bob@example.com' }
    });
    fireEvent.change(screen.getByLabelText(/^password/i), {
      target: { value: 'short' }
    });
    fireEvent.change(screen.getByLabelText(/^confirm password/i), {
      target: { value: 'mismatch' }
    });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    });

    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('submits registration successfully and shows success message', async () => {
    mockRegister.mockResolvedValueOnce({
      success: true,
      message: 'User registered successfully. Please verify your email.'
    });

    renderComponent();

    fireEvent.change(screen.getByLabelText(/^full name/i), {
      target: { value: 'Bob Smith' }
    });
    fireEvent.change(screen.getByLabelText(/^username/i), {
      target: { value: 'bob_smith' }
    });
    fireEvent.change(screen.getByLabelText(/^email address/i), {
      target: { value: 'bob@example.com' }
    });
    fireEvent.change(screen.getByLabelText(/^password/i), {
      target: { value: 'ValidPass123!' }
    });
    fireEvent.change(screen.getByLabelText(/^confirm password/i), {
      target: { value: 'ValidPass123!' }
    });

    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        fullName: 'Bob Smith',
        username: 'bob_smith',
        email: 'bob@example.com',
        password: 'ValidPass123!'
      });
      expect(screen.getByText(/account registered!/i)).toBeInTheDocument();
      expect(screen.getByText(/user registered successfully/i)).toBeInTheDocument();
    });
  });
});
