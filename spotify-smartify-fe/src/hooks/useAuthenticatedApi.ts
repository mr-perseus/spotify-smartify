import { useCallback, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi, TimeRange } from '../services/userApi';
import { authApi, AuthTokens } from '../services/authApi';
import { UnauthorizedError } from '../services/errors';

/**
 * Returns API functions that automatically retry with a fresh access token on 401.
 * If the token refresh also fails, logout() is called.
 */
export function useAuthenticatedApi() {
  const { tokens, saveTokens, logout } = useAuth();

  // Keep a ref to the latest tokens so withRefresh doesn't need tokens as a dep
  // (avoids re-creating getProfile/getTopTracks every time a refresh updates tokens).
  const tokensRef = useRef<AuthTokens | null>(tokens);
  useEffect(() => {
    tokensRef.current = tokens;
  }, [tokens]);

  const withRefresh = useCallback(
    async <T>(fn: (token: string) => Promise<T>): Promise<T> => {
      const current = tokensRef.current;
      if (!current) {
        logout();
        throw new UnauthorizedError();
      }

      try {
        return await fn(current.accessToken);
      } catch (err) {
        if (err instanceof UnauthorizedError && current.refreshToken) {
          try {
            const refreshed = await authApi.refreshToken(current.refreshToken);
            const newTokens: AuthTokens = {
              ...current,
              accessToken: refreshed.accessToken,
              expiresIn: refreshed.expiresIn,
            };
            saveTokens(newTokens);
            tokensRef.current = newTokens;
            return await fn(newTokens.accessToken);
          } catch {
            logout();
            throw new UnauthorizedError();
          }
        }
        throw err;
      }
    },
    [saveTokens, logout],
  );

  return {
    getProfile: useCallback(() => withRefresh(token => userApi.getProfile(token)), [withRefresh]),
    getTopTracks: useCallback(
      (timeRange: TimeRange) => withRefresh(token => userApi.getTopTracks(token, timeRange)),
      [withRefresh],
    ),
    getPlaylistTracks: useCallback(
      (playlistId: string) => withRefresh(token => userApi.getPlaylistTracks(token, playlistId)),
      [withRefresh],
    ),
  };
}
