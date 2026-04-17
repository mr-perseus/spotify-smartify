import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { AuthTokens } from '../services/authApi';

interface AuthContextValue {
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  saveTokens: (tokens: AuthTokens) => void;
  logout: () => void;
}

const AUTH_STORAGE_KEY = 'spotify_auth';

function loadTokensFromStorage(): AuthTokens | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [tokens, setTokens] = useState<AuthTokens | null>(loadTokensFromStorage);

  const saveTokens = useCallback((newTokens: AuthTokens) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(newTokens));
    setTokens(newTokens);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setTokens(null);
  }, []);

  return (
    <AuthContext.Provider value={{ tokens, isAuthenticated: tokens !== null, saveTokens, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
