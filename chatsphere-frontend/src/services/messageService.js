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
    return response.data;
  },

  /**
   * REST fallback for sending a message
   * @param {{ conversationPublicId: string, clientMessageId: string, content: string, messageType?: string, attachmentPublicIds?: string[], replyToMessagePublicId?: string }} payload
   */
  async sendMessage({ conversationPublicId, clientMessageId, content, messageType = 'TEXT', attachmentPublicIds = [], replyToMessagePublicId = null }) {
    const response = await api.post('/messages', {
      conversationPublicId,
      clientMessageId,
      content,
      messageType,
      attachmentPublicIds,
      replyToMessagePublicId
    });
    return response.data.data;
  },

  /**
   * Edit sender's own message
   */
  async editMessage(publicId, content) {
    const response = await api.put(`/messages/${publicId}`, { content });
    return response.data.data;
  },

  /**
   * Delete message
   * @param {string} publicId
   * @param {boolean} forEveryone
   */
  async deleteMessage(publicId, forEveryone = false) {
    const response = await api.delete(`/messages/${publicId}`, {
      params: { forEveryone }
    });
    return response.data;
  },

  /**
   * Forward message to target conversation
   */
  async forwardMessage(publicId, targetConversationPublicId) {
    const response = await api.post(`/messages/${publicId}/forward`, {
      targetConversationPublicId
    });
    return response.data.data;
  },

  /**
   * Add or update reaction
   */
  async addReaction(publicId, emoji) {
    const response = await api.post(`/messages/${publicId}/reactions`, { emoji });
    return response.data.data;
  },

  /**
   * Remove reaction
   */
  async removeReaction(publicId) {
    const response = await api.delete(`/messages/${publicId}/reactions`);
    return response.data.data;
  },

  /**
   * Pin message
   */
  async pinMessage(publicId) {
    const response = await api.post(`/messages/${publicId}/pin`);
    return response.data.data;
  },

  /**
   * Unpin message
   */
  async unpinMessage(publicId) {
    const response = await api.delete(`/messages/${publicId}/pin`);
    return response.data.data;
  },

  /**
   * Get pinned messages
   */
  async getPinnedMessages(conversationPublicId) {
    const response = await api.get(`/conversations/${conversationPublicId}/pinned`);
    return response.data.data;
  },

  /**
   * Search messages across accessible conversations
   */
  async searchMessages({ conversationId, query, startDate, endDate, page = 0, size = 20 } = {}) {
    const params = { query, page, size };
    if (conversationId) params.conversationId = conversationId;
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;

    const response = await api.get('/messages/search', { params });
    return response.data.data;
  },

  /**
   * Delete conversation history for me
   */
  async deleteConversationForMe(conversationPublicId) {
    const response = await api.delete(`/conversations/${conversationPublicId}`);
    return response.data;
  }
};

export default messageService;
