// src/api/health.api.js
import axios from 'axios';
import { getConfig } from '../utils/config';

async function checkHealth(url) {
  try {
    const { data } = await axios.get(`${url}/actuator/health`, { timeout: 5000 });
    return { status: data.status === 'UP' ? 'UP' : 'DOWN', details: data };
  } catch (err) {
    return { status: 'DOWN', error: err.message };
  }
}

export const healthApi = {
  checkAll: async () => {
    const cfg = getConfig();
    const [gateway, user, order, product, notification] = await Promise.allSettled([
      checkHealth(cfg.gatewayUrl),
      checkHealth(cfg.userServiceUrl),
      checkHealth(cfg.orderServiceUrl),
      checkHealth(cfg.productServiceUrl),
      checkHealth(cfg.notificationServiceUrl),
    ]);
    return {
      gateway: gateway.value || { status: 'DOWN' },
      user: user.value || { status: 'DOWN' },
      order: order.value || { status: 'DOWN' },
      product: product.value || { status: 'DOWN' },
      notification: notification.value || { status: 'DOWN' },
    };
  },
  checkService: (url) => checkHealth(url),
};

// src/api/aggregator.api.js
import { gatewayClient } from './axiosInstance';
export const aggregatorApi = {
  getOrderDetails: (orderId) =>
    gatewayClient.get(`/api/v1/aggregator/order-details/${orderId}`),
};
