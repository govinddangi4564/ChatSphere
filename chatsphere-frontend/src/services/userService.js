import api from './api';

export const userService = {
  getMe: async () => {
    try {
      const response = await api.get('/users/me');
      return response.data?.data;
    } catch {
      // /api/users/me will be formally added in Phase 3. Return null gracefully for now.
      return null;
    }
  }
};
