// src/utils/errorHelper.js

/**
 * Checks if an error is due to backend services being offline or unreachable.
 */
export function isServiceOfflineError(err) {
  if (!err) return false;
  if (!err.response) return true;
  const status = err.response?.status;
  return err.code === 'ERR_NETWORK' ||
         err.code === 'ECONNREFUSED' ||
         [502, 503, 504].includes(status) ||
         err.message?.includes('Network Error') ||
         err.message?.includes('Failed to fetch');
}

/**
 * Formats an API error for login attempts.
 * Differentiates clearly between offline/unreachable services and invalid credentials.
 */
export function getLoginErrorMessage(err) {
  if (!err.response || err.code === 'ERR_NETWORK' || err.code === 'ECONNREFUSED' || err.message?.includes('Network Error')) {
    return '🚨 Backend services are currently offline or unreachable. Please ensure API Gateway and Microservices are running.';
  }

  const status = err.response?.status;
  if ([502, 503, 504].includes(status)) {
    return `🚨 User Service or API Gateway is temporarily unavailable (HTTP ${status}). Please check backend service health.`;
  }

  if (status >= 500) {
    const serverMsg = err.response?.data?.message || err.response?.data?.errorDesc;
    return `🚨 Backend error (HTTP ${status}): ${serverMsg || 'The authentication service encountered an error.'}`;
  }

  if (status === 401 || status === 400 || status === 404) {
    return err.response?.data?.message || err.response?.data?.errorDesc || '❌ Invalid Email / User ID or Password. Please check your credentials.';
  }

  return err.response?.data?.message || err.response?.data?.errorDesc || '❌ Login failed. Please try again.';
}

/**
 * Formats an API error for registration attempts.
 */
export function getRegisterErrorMessage(err) {
  if (!err.response || err.code === 'ERR_NETWORK' || err.code === 'ECONNREFUSED' || err.message?.includes('Network Error')) {
    return '🚨 Cannot register: Backend services are offline or unreachable. Please start API Gateway and User Service.';
  }

  const status = err.response?.status;
  if ([502, 503, 504].includes(status)) {
    return `🚨 User Service is temporarily unavailable (HTTP ${status}). Please try again later.`;
  }

  if (status >= 500) {
    const serverMsg = err.response?.data?.message || err.response?.data?.errorDesc;
    return `🚨 Server error (HTTP ${status}): ${serverMsg || 'Registration failed due to a server error.'}`;
  }

  return err.response?.data?.errorDesc || err.response?.data?.message || `Registration failed (HTTP ${status || 'Error'}).`;
}

/**
 * Formats a general API error message with fallback.
 */
export function getServiceErrorMessage(err, defaultMsg = 'An unexpected error occurred.') {
  if (!err) return defaultMsg;
  if (!err.response || err.code === 'ERR_NETWORK' || err.code === 'ECONNREFUSED' || err.message?.includes('Network Error')) {
    return '🚨 Backend service is currently offline or unreachable. Please check service health.';
  }

  const status = err.response?.status;
  if ([502, 503, 504].includes(status)) {
    return `🚨 Backend microservice is temporarily unavailable (HTTP ${status}).`;
  }

  if (status >= 500) {
    const serverMsg = err.response?.data?.message || err.response?.data?.errorDesc;
    return `🚨 Server Error (${status}): ${serverMsg || defaultMsg}`;
  }

  return err.response?.data?.errorDesc || err.response?.data?.message || err.response?.data?.error || defaultMsg;
}
