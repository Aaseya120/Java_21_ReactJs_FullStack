// src/api/axiosInstance.js
import axios from 'axios';
import { getAccessToken, getRefreshToken, saveTokens, clearTokens } from '../utils/token';
import { getGatewayUrl } from '../utils/config';

let isRefreshing = false;
let failedQueue = [];

function processQueue(error, token = null) {
  failedQueue.forEach(prom => {
    if (error) prom.reject(error);
    else prom.resolve(token);
  });
  failedQueue = [];
}

function createInstance(baseURLFn) {
  const instance = axios.create({
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' },
  });

  instance.interceptors.request.use(config => {
    config.baseURL = typeof baseURLFn === 'function' ? baseURLFn() : baseURLFn;
    const token = getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  instance.interceptors.response.use(
    response => response,
    async error => {
      if (error.code === 'ERR_NETWORK' || [502, 503, 504].includes(error.response?.status)) {
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      const originalRequest = error.config;
      if (error.response?.status === 401 && !originalRequest._retry) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          }).then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return instance(originalRequest);
          });
        }
        originalRequest._retry = true;
        isRefreshing = true;
        const refreshToken = getRefreshToken();
        if (!refreshToken) {
          clearTokens();
          window.location.href = '/login';
          return Promise.reject(error);
        }
        try {
          const { data } = await axios.post(
            `${getGatewayUrl()}/api/v1/auth/refresh`,
            { refreshToken }
          );
          const tokens = data.data;
          saveTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
          processQueue(null, tokens.accessToken);
          originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`;
          return instance(originalRequest);
        } catch (err) {
          processQueue(err, null);
          clearTokens();
          window.location.href = '/login';
          return Promise.reject(err);
        } finally {
          isRefreshing = false;
        }
      }
      return Promise.reject(error);
    }
  );

  return instance;
}

// Gateway client (all standard requests go through here)
export const gatewayClient = createInstance(() => getGatewayUrl());

// Per-service direct clients (for direct access / health checks)
import { getConfig } from '../utils/config';
export const userClient = createInstance(() => getConfig().userServiceUrl);
export const orderClient = createInstance(() => getConfig().orderServiceUrl);
export const productClient = createInstance(() => getConfig().productServiceUrl);
export const notificationClient = createInstance(() => getConfig().notificationServiceUrl);
