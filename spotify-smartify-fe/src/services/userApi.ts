const API_BASE_URL = 'http://127.0.0.1:8080';

export class UnauthorizedError extends Error {
  constructor() {
    super('unauthorized');
    this.name = 'UnauthorizedError';
  }
}

async function fetchOrThrow(url: string): Promise<any> {
  const response = await fetch(url);
  if (response.status === 401) throw new UnauthorizedError();
  if (!response.ok) throw new Error(`Request failed: ${response.status}`);
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

export const userApi = {
  getProfile: async (): Promise<UserProfile> => {
    return fetchOrThrow(`${API_BASE_URL}/user/profile`);
  },

  getTopTracks: async (): Promise<TopTrack[]> => {
    return fetchOrThrow(`${API_BASE_URL}/user/top-tracks`);
  },
};
