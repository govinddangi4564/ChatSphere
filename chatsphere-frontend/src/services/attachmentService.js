import api from './api';

export const attachmentService = {
  /**
   * Upload an attachment file with upload progress tracking
   * @param {File} file
   * @param {Function} [onProgress] (progressEvent) => void
   * @param {AbortSignal} [signal]
   * @returns {Promise<Object>} AttachmentResponse
   */
  async uploadAttachment(file, onProgress, signal) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post('/attachments/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      signal,
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onProgress(percentCompleted);
        }
      },
    });

    return response.data?.data;
  },

  /**
   * Get direct URL or API URL for attachment
   * @param {string} publicId
   * @returns {string}
   */
  getAttachmentUrl(publicId) {
    const baseURL = api.defaults.baseURL || '/api';
    return `${baseURL}/attachments/${publicId}`;
  },

  /**
   * Delete an attachment
   * @param {string} publicId
   * @returns {Promise<void>}
   */
  async deleteAttachment(publicId) {
    await api.delete(`/attachments/${publicId}`);
  },
};

export default attachmentService;
