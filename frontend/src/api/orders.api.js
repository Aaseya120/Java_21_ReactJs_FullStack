// src/api/orders.api.js
import { gatewayClient } from './axiosInstance';
import { v4 as uuidv4 } from 'uuid';

export const ordersApi = {
  create: (data) =>
    gatewayClient.post('/api/v1/orders', data, {
      headers: { 'Idempotency-Key': uuidv4() },
    }),
  getAll: (params) => gatewayClient.get('/api/v1/orders', { params }),
  getById: (id) => gatewayClient.get(`/api/v1/orders/${id}`),
  getByUserId: (userId) => gatewayClient.get(`/api/v1/orders/user/${userId}`),
  updateStatus: (id, status) =>
    gatewayClient.put(`/api/v1/orders/${id}/status`, null, { params: { status } }),
  cancel: (id) => gatewayClient.delete(`/api/v1/orders/${id}`),
  refund: (id, data) => gatewayClient.post(`/api/v1/orders/${id}/refund`, data),
};
