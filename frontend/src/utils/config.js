// src/utils/config.js
const STORAGE_KEY = 'ms_config';

const DEFAULTS = {
  gatewayUrl: 'http://localhost:8080',
  userServiceUrl: 'http://localhost:8081',
  orderServiceUrl: 'http://localhost:8082',
  productServiceUrl: 'http://localhost:8083',
  notificationServiceUrl: 'http://localhost:8084',
};

export function getConfig() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? { ...DEFAULTS, ...JSON.parse(stored) } : { ...DEFAULTS };
  } catch {
    return { ...DEFAULTS };
  }
}

export function saveConfig(config) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
}

export function resetConfig() {
  localStorage.removeItem(STORAGE_KEY);
  return { ...DEFAULTS };
}

export function getGatewayUrl() {
  return getConfig().gatewayUrl;
}
