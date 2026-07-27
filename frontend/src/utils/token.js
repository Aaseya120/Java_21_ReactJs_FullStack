// src/utils/token.js
export const TOKEN_KEY = 'ms_access_token';
export const REFRESH_KEY = 'ms_refresh_token';
export const USER_KEY = 'ms_user';

export function saveTokens({ accessToken, refreshToken }) {
  if (accessToken) sessionStorage.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) sessionStorage.setItem(REFRESH_KEY, refreshToken);
}

export function getAccessToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_KEY);
}

export function clearTokens() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function saveUser(user) {
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getUser() {
  try {
    const raw = sessionStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function decodeToken(token) {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

export function isTokenExpired(token) {
  const decoded = decodeToken(token);
  if (!decoded || !decoded.exp) return true;
  return decoded.exp * 1000 < Date.now();
}
