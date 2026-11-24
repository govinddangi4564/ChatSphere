import React, { createContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../services/authService';
import { userService } from '../services/userService';
import { setAccessToken, getAccessToken } from '../services/api';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchUserProfile = useCallback(async (fallbackUser = null) => {
    try {
      const profile = await userService.getMe();
      if (profile) {
        setUser(profile);
      } else if (fallbackUser) {
        setUser(fallbackUser);
      }
    } catch {
      if (fallbackUser) {
        setUser(fallbackUser);
      }
    }
  }, []);

  // Silent refresh on initial app load to restore active session from HttpOnly cookie
  useEffect(() => {
    let isMounted = true;

    const restoreSession = async () => {
      try {
        const response = await authService.refresh();
        if (isMounted && response?.data) {
          const authData = response.data;
          setAccessToken(authData.accessToken);
          await fetchUserProfile(authData.user);
        }
      } catch {
        // No active session or expired cookie
        if (isMounted) {
          setAccessToken(null);
          setUser(null);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    restoreSession();

    return () => {
      isMounted = false;
    };
  }, [fetchUserProfile]);

  const login = async (credentials) => {
    const response = await authService.login(credentials);
    const authData = response?.data;
    if (authData) {
      setAccessToken(authData.accessToken);
      await fetchUserProfile(authData.user);
    }
    return response;
  };

  const register = async (userData) => {
    return await authService.register(userData);
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (err) {
      console.warn('Logout request failed:', err);
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  };

  const value = {
    user,
    accessToken: getAccessToken(),
    loading,
    isAuthenticated: Boolean(user),
    login,
    register,
    logout,
    setUser
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
