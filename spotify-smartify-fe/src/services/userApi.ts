import { UnauthorizedError } from './errors';
import { API_BASE_URL } from './config';

export { UnauthorizedError } from './errors';

export type TimeRange = 'short_term' | 'medium_term' | 'long_term';

export const TIME_RANGE_LABELS: Record<TimeRange, string> = {
  short_term: 'Last 4 weeks',
  medium_term: 'Last 6 months',
  long_term: 'All time',
};

async function fetchOrThrow(url: string, accessToken: string): Promise<any> {
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (response.status === 401) throw new UnauthorizedError();
  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    try {
      const body = await response.json();
      if (body.message) message = body.message;
    } catch {
      // response wasn't JSON, keep the default message
    }
    throw new Error(message);
  }
  return response.json();
}

export interface TopTrack {
  id: string;
  name: string;
  artists: string;
  albumName: string;
  albumImageUrl: string;
  previewUrl: string;
  spotifyUrl: string;
}

export interface UserProfile {
  displayName: string;
  email: string;
}

export interface PlaylistInfo {
  playlistName: string;
  tracks: TopTrack[];
}

export const userApi = {
  getProfile: async (accessToken: string): Promise<UserProfile> => {
    return fetchOrThrow(`${API_BASE_URL}/user/profile`, accessToken);
  },

  getTopTracks: async (accessToken: string, timeRange: TimeRange = 'medium_term'): Promise<TopTrack[]> => {
    return fetchOrThrow(`${API_BASE_URL}/user/top-tracks?timeRange=${timeRange}`, accessToken);
  },

  getPlaylistTracks: async (accessToken: string, playlistId: string): Promise<PlaylistInfo> => {
    return fetchOrThrow(`${API_BASE_URL}/playlist/${playlistId}/tracks`, accessToken);
  },
};
