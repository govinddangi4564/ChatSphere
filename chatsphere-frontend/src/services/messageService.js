import api from './api';

export const messageService = {
  /**
   * Fetch conversation messages with keyset cursor pagination
   * @param {string} conversationPublicId
   * @param {{ size?: number, before?: string }} params
   */
  async getMessages(conversationPublicId, { size = 30, before } = {}) {
    const params = { size };
    if (before) {
      params.before = before;
    }
    const response = await api.get(`/conversations/${conversationPublicId}/messages`, { params });
    return response.data; // CursorApiResponse: { success, message, data, nextCursor, hasMore }
  },

  /**
   * REST fallback for sending a message
   * @param {{ conversationPublicId: string, clientMessageId: string, content: string, messageType?: string }} payload
   */
  async sendMessage({ conversationPublicId, clientMessageId, content, messageType = 'TEXT' }) {
    const response = await api.post('/messages', {
      conversationPublicId,
      clientMessageId,
      content,
      messageType
    });
    return response.data.data;
  }
};
