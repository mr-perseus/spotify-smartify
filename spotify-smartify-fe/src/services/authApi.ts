import { UnauthorizedError } from './errors';
import { API_BASE_URL } from './config';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface RefreshResult {
  accessToken: string;
  expiresIn: number;
}

export const authApi = {
  getLoginUrl: async (): Promise<string> => {
    const response = await fetch(`${API_BASE_URL}/auth/login`);
    if (!response.ok) throw new Error('Failed to get login URL');
    const data = await response.json();
    return data.authorizationUrl;
  },

  refreshToken: async (refreshToken: string): Promise<RefreshResult> => {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (response.status === 401) throw new UnauthorizedError();
    if (!response.ok) throw new Error('Failed to refresh token');
    return response.json();
  },
};
