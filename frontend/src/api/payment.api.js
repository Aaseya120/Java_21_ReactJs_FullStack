// src/api/payment.api.js
import axiosInstance from './axiosInstance';

export const paymentApi = {
  getPublicKey: () =>
    axiosInstance.get('/api/v1/payments/security/public-key'),

  processPayment: (data) =>
    axiosInstance.post('/api/v1/payments', data),

  getById: (id) =>
    axiosInstance.get(`/api/v1/payments/${id}`),

  getByOrderId: (orderId) =>
    axiosInstance.get(`/api/v1/payments/order/${orderId}`),

  getByUserId: (userId) =>
    axiosInstance.get(`/api/v1/payments/user/${userId}`),

  refundPayment: (id, data) =>
    axiosInstance.post(`/api/v1/payments/${id}/refund`, data),
};
