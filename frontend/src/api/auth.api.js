// src/api/auth.api.js
import { gatewayClient } from './axiosInstance';

export const authApi = {
  register: (data) => gatewayClient.post('/api/v1/auth/register', data),
  login: (data) => gatewayClient.post('/api/v1/auth/login', data),
  refresh: (refreshToken) => gatewayClient.post('/api/v1/auth/refresh', { refreshToken }),
  logout: (refreshToken) => gatewayClient.post('/api/v1/auth/logout', { refreshToken }),
  recoverId: (contact) => gatewayClient.get('/api/v1/auth/recover-id', { params: { contact } }),
};

// src/api/users.api.js — exported separately below

// src/api/products.api.js — exported separately below
