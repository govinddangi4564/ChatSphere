import api from './api';

export const groupService = {
  createGroup: async (data) => {
    const res = await api.post('/api/groups', data);
    return res.data?.data;
  },

  getGroupDetails: async (publicId) => {
    const res = await api.get(`/api/groups/${publicId}`);
    return res.data?.data;
  },

  updateGroup: async (publicId, data) => {
    const res = await api.put(`/api/groups/${publicId}`, data);
    return res.data?.data;
  },

  addMembers: async (publicId, memberPublicIds) => {
    const res = await api.post(`/api/groups/${publicId}/members`, { memberPublicIds });
    return res.data?.data;
  },

  removeMember: async (publicId, userPublicId) => {
    const res = await api.delete(`/api/groups/${publicId}/members/${userPublicId}`);
    return res.data?.data;
  },

  leaveGroup: async (publicId) => {
    const res = await api.post(`/api/groups/${publicId}/leave`);
    return res.data?.data;
  },

  updateMemberRole: async (publicId, userPublicId, role) => {
    const res = await api.put(`/api/groups/${publicId}/admins/${userPublicId}`, { role });
    return res.data?.data;
  }
};
