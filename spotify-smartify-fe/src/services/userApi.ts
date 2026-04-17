const API_BASE_URL = 'http://127.0.0.1:8080';

export interface TopTrack {
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
    const response = await fetch(`${API_BASE_URL}/user/profile`);
    if (!response.ok) throw new Error('Failed to fetch profile');
    return response.json();
  },

  getTopTrack: async (): Promise<TopTrack> => {
    const response = await fetch(`${API_BASE_URL}/user/top-track`);
    if (!response.ok) throw new Error('Failed to fetch top track');
    return response.json();
  },
};
