/* Spotify Web Playback SDK type declarations */

interface Window {
  onSpotifyWebPlaybackSDKReady: () => void;
  Spotify: typeof Spotify;
}

declare namespace Spotify {
  class Player {
    constructor(options: PlayerOptions);
    connect(): Promise<boolean>;
    disconnect(): void;
    addListener(event: 'ready', callback: (state: { device_id: string }) => void): void;
    addListener(event: 'not_ready', callback: (state: { device_id: string }) => void): void;
    addListener(event: 'player_state_changed', callback: (state: PlaybackState | null) => void): void;
    addListener(event: 'autoplay_failed', callback: () => void): void;
    addListener(event: 'initialization_error', callback: (error: { message: string }) => void): void;
    addListener(event: 'authentication_error', callback: (error: { message: string }) => void): void;
    addListener(event: 'account_error', callback: (error: { message: string }) => void): void;
    addListener(event: 'playback_error', callback: (error: { message: string }) => void): void;
    removeListener(event: string, callback?: (...args: any[]) => void): void;
    getCurrentState(): Promise<PlaybackState | null>;
    pause(): Promise<void>;
    resume(): Promise<void>;
    togglePlay(): Promise<void>;
    seek(positionMs: number): Promise<void>;
    setVolume(volume: number): Promise<void>;
    getVolume(): Promise<number>;
    activateElement(): Promise<void>;
  }

  interface PlayerOptions {
    name: string;
    getOAuthToken: (cb: (token: string) => void) => void;
    volume?: number;
  }

  interface PlaybackState {
    paused: boolean;
    position: number;
    duration: number;
    track_window: {
      current_track: WebPlaybackTrack;
    };
  }

  interface WebPlaybackTrack {
    uri: string;
    id: string;
    name: string;
    artists: Array<{ name: string; uri: string }>;
    album: {
      name: string;
      uri: string;
      images: Array<{ url: string }>;
    };
  }
}
