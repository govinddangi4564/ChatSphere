import api from './api';

export const conversationService = {
  getConversations: async (params = {}) => {
    const response = await api.get('/conversations', { params });
    return response.data?.data;
  },

  getConversation: async (publicId) => {
    const response = await api.get(`/conversations/${publicId}`);
    return response.data?.data;
  },

  createOrFindPrivateConversation: async (recipientPublicId) => {
    const response = await api.post('/conversations/private', { recipientPublicId });
    return response.data?.data;
  },

  archiveConversation: async (publicId, archived = true) => {
    const response = await api.put(`/conversations/${publicId}/archive`, { archived });
    return response.data;
  },

  muteConversation: async (publicId, muted = true) => {
    const response = await api.put(`/conversations/${publicId}/mute`, { muted });
    return response.data;
  }
};
