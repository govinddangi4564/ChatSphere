import api, { setAccessToken } from './api';

export const authService = {
  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },

  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    const authData = response.data?.data;
    if (authData?.accessToken) {
      setAccessToken(authData.accessToken);
    }
    return response.data;
  },

  refresh: async () => {
    const response = await api.post('/auth/refresh');
    const authData = response.data?.data;
    if (authData?.accessToken) {
      setAccessToken(authData.accessToken);
    }
    return response.data;
  },

  logout: async () => {
    try {
      const response = await api.post('/auth/logout');
      return response.data;
    } finally {
      setAccessToken(null);
    }
  },

  verifyEmail: async (token) => {
    const response = await api.post('/auth/verify-email', { token });
    return response.data;
  },

  forgotPassword: async (email) => {
    const response = await api.post('/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (token, newPassword) => {
    const response = await api.post('/auth/reset-password', { token, newPassword });
    return response.data;
  }
};
