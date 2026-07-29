// src/context/AuthContext.jsx
import { createContext, useContext, useState, useCallback } from 'react';
import { saveTokens, clearTokens, saveUser, getUser, getAccessToken, getRefreshToken } from '../utils/token';
import { authApi } from '../api/auth.api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getUser());
  const [isAuthenticated, setIsAuthenticated] = useState(() => !!getAccessToken());

  const login = useCallback(async (credentials) => {
    const { data } = await authApi.login(credentials);
    const payload = data.data;
    saveTokens({ accessToken: payload.accessToken, refreshToken: payload.refreshToken });
    saveUser(payload.user);
    setUser(payload.user);
    setIsAuthenticated(true);
    return payload;
  }, []);

  const register = useCallback(async (formData) => {
    const { data } = await authApi.register(formData);
    const payload = data.data;
    if (!payload?.accessToken) {
      throw new Error('Registration succeeded but no token returned — check backend response.');
    }
    saveTokens({ accessToken: payload.accessToken, refreshToken: payload.refreshToken });
    saveUser(payload.user);
    setUser(payload.user);
    setIsAuthenticated(true);
    return payload;
  }, []);

  const registerWithoutLogin = useCallback(async (formData) => {
    const { data } = await authApi.register(formData);
    const payload = data.data;
    return payload?.user || payload;
  }, []);

  const logout = useCallback(async () => {
    const rt = getRefreshToken();
    
    clearTokens();
    setUser(null);
    setIsAuthenticated(false);

    try {
      if (rt) await authApi.logout(rt);
    } catch (err) {
      console.warn('Logout API failed:', err);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, register, registerWithoutLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
