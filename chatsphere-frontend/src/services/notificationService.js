import api from './api';

export const notificationService = {
  /**
   * Fetch paginated notifications with optional read filter
   * @param {Object} params - { page, size, sort, read }
   */
  async getNotifications(params = {}) {
    const response = await api.get('/notifications', { params });
    return response.data?.data;
  },

  /**
   * Get total unread notifications count
   */
  async getUnreadCount() {
    const response = await api.get('/notifications/unread-count');
    return response.data?.data?.unreadCount ?? 0;
  },

  /**
   * Mark a single notification as read
   * @param {string} publicId
   */
  async markAsRead(publicId) {
    const response = await api.put(`/notifications/${publicId}/read`);
    return response.data?.data;
  },

  /**
   * Mark all notifications as read
   */
  async markAllAsRead() {
    const response = await api.put('/notifications/read-all');
    return response.data;
  }
};
