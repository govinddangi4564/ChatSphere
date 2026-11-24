import api from './api';

export const contactService = {
  getContacts: async () => {
    const response = await api.get('/contacts');
    return response.data?.data;
  },

  addContact: async (publicId) => {
    const response = await api.post(`/contacts/${publicId}`);
    return response.data?.data;
  },

  removeContact: async (publicId) => {
    const response = await api.delete(`/contacts/${publicId}`);
    return response.data;
  }
};
