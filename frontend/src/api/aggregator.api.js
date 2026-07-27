// src/api/aggregator.api.js
import { gatewayClient } from './axiosInstance';

export const aggregatorApi = {
  getOrderDetails: (orderId) =>
    gatewayClient.get(`/api/v1/aggregator/order-details/${orderId}`),
};
