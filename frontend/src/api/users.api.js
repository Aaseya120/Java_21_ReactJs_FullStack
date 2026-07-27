// src/api/users.api.js
import { gatewayClient } from './axiosInstance';

export const usersApi = {
  getById: (id) => gatewayClient.get(`/api/v1/users/${id}`),
  updateName: (id, fullName) =>
    gatewayClient.put(`/api/v1/users/${id}/name`, null, { params: { fullName } }),
};
