// src/api/products.api.js
import { gatewayClient } from './axiosInstance';

export const productsApi = {
  create: (data) => gatewayClient.post('/api/v1/products', data),
  getAll: (params) => gatewayClient.get('/api/v1/products', { params }),
  getById: (id) => gatewayClient.get(`/api/v1/products/${id}`),
  getByCategory: (category) => gatewayClient.get(`/api/v1/products/category/${category}`),
  update: (id, data) => gatewayClient.put(`/api/v1/products/${id}`, data),
  delete: (id) => gatewayClient.delete(`/api/v1/products/${id}`),
  addStock: (id, quantity) =>
    gatewayClient.post(`/api/v1/products/${id}/stock/add`, null, { params: { quantity } }),
  deductStock: (id, quantity) =>
    gatewayClient.post(`/api/v1/products/${id}/stock/deduct`, null, { params: { quantity } }),
};
