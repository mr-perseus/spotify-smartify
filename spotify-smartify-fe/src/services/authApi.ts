const API_BASE_URL = 'http://127.0.0.1:8080';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export const authApi = {
  getLoginUrl: async (): Promise<string> => {
    const response = await fetch(`${API_BASE_URL}/auth/login`);
    if (!response.ok) throw new Error('Failed to get login URL');
    const data = await response.json();
    return data.authorizationUrl;
  },

  exchangeCode: async (code: string): Promise<AuthTokens> => {
    const response = await fetch(`${API_BASE_URL}/auth/callback?code=${encodeURIComponent(code)}`);
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.error || 'Authentication failed');
    }
    return response.json();
  },
};
