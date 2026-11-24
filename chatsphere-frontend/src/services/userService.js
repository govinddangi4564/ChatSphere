import api from './api';

export const userService = {
  getMe: async () => {
    const response = await api.get('/users/me');
    return response.data?.data;
  },

  updateProfile: async (data) => {
    const response = await api.put('/users/me', data);
    return response.data?.data;
  },

  searchUsers: async (query = '', page = 0, size = 20, sort = 'username,asc') => {
    const response = await api.get('/users/search', {
      params: { query, page, size, sort }
    });
    return response.data?.data;
  },

  getUserProfile: async (publicId) => {
    const response = await api.get(`/users/${publicId}`);
    return response.data?.data;
  },

  blockUser: async (publicId) => {
    const response = await api.post(`/users/${publicId}/block`);
    return response.data;
  },

  unblockUser: async (publicId) => {
    const response = await api.delete(`/users/${publicId}/block`);
    return response.data;
  },

  getBlockedUsers: async () => {
    const response = await api.get('/users/blocked');
    return response.data?.data;
  },

  getPrivacySettings: async () => {
    const response = await api.get('/users/privacy');
    return response.data?.data;
  },

  updatePrivacySettings: async (settings) => {
    const response = await api.put('/users/privacy', settings);
    return response.data?.data;
  },

  sendHeartbeat: async () => {
    const response = await api.post('/users/presence/heartbeat');
    return response.data;
  }
};
