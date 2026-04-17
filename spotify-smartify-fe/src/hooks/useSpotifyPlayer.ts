import { useEffect, useRef, useState, useCallback } from 'react';

const SPOTIFY_API_BASE = 'https://api.spotify.com/v1/me/player';

export interface SpotifyPlayerHandle {
  /** true once the SDK device is registered and ready */
  isReady: boolean;
  /** true while a full Spotify track is playing via the SDK */
  isPlaying: boolean;
  /** Play a full track. Returns true if playback started via SDK, false if unavailable. */
  play: (trackId: string) => Promise<boolean>;
  /** Pause SDK playback. */
  pause: () => Promise<void>;
  /** Resume SDK playback. */
  resume: () => Promise<void>;
  /** Disconnect the SDK player (call on unmount / page leave). */
  disconnect: () => void;
}

/**
 * Manages a Spotify Web Playback SDK player instance.
 *
 * `getAccessToken` is called whenever the SDK or the play() function needs a
 * fresh token — it should return the latest access token from auth context.
 *
 * Falls back gracefully: if the SDK fails to load (ad-blocker, no Premium, etc.),
 * `isReady` stays false and `play()` returns false so the caller can fall back to previews.
 */
export function useSpotifyPlayer(getAccessToken: () => string | null): SpotifyPlayerHandle {
  const [isReady, setIsReady] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const playerRef = useRef<Spotify.Player | null>(null);
  const deviceIdRef = useRef<string | null>(null);
  const deviceActivatedRef = useRef(false);
  const getTokenRef = useRef(getAccessToken);

  // Always keep the token getter current
  useEffect(() => {
    getTokenRef.current = getAccessToken;
  }, [getAccessToken]);

  useEffect(() => {
    let mounted = true;

    function initPlayer() {
      const player = new window.Spotify.Player({
        name: 'Spotify Smartify',
        getOAuthToken: (cb) => {
          const token = getTokenRef.current();
          if (token) cb(token);
        },
        volume: 0.5,
      });

      player.addListener('ready', ({ device_id }) => {
        if (!mounted) return;
        console.log('[SpotifyPlayer] ready, device_id:', device_id);
        deviceIdRef.current = device_id;
        deviceActivatedRef.current = false;
        setIsReady(true);

        // Proactively transfer playback to register the device with Spotify's API.
        // play: false keeps it silent — just makes the device "known".
        const token = getTokenRef.current();
        if (token) {
          transferPlayback(device_id, token).then((ok) => {
            if (ok) {
              console.log('[SpotifyPlayer] device activated via transfer');
              deviceActivatedRef.current = true;
            }
          });
        }
      });

      player.addListener('not_ready', () => {
        if (!mounted) return;
        console.log('[SpotifyPlayer] not_ready');
        deviceIdRef.current = null;
        deviceActivatedRef.current = false;
        setIsReady(false);
      });

      player.addListener('player_state_changed', (state) => {
        if (!mounted) return;
        if (!state) {
          setIsPlaying(false);
          return;
        }
        setIsPlaying(!state.paused);
      });

      player.addListener('initialization_error', ({ message }) => {
        console.warn('[SpotifyPlayer] init error:', message);
      });
      player.addListener('authentication_error', ({ message }) => {
        console.warn('[SpotifyPlayer] auth error:', message);
        if (mounted) setIsReady(false);
      });
      player.addListener('account_error', ({ message }) => {
        console.warn('[SpotifyPlayer] account error (Premium required):', message);
        if (mounted) setIsReady(false);
      });
      player.addListener('playback_error', ({ message }) => {
        console.warn('[SpotifyPlayer] playback error:', message);
      });

      // activateElement pre-warms the audio element so browsers allow playback.
      player.activateElement();
      player.connect().then((success) => {
        if (!success) {
          console.warn('[SpotifyPlayer] connect() returned false');
        }
      });
      playerRef.current = player;
    }

    // The SDK script may already be loaded (onSpotifyWebPlaybackSDKReady already fired)
    if (window.Spotify?.Player) {
      initPlayer();
    } else {
      window.onSpotifyWebPlaybackSDKReady = () => {
        if (mounted) initPlayer();
      };
    }

    return () => {
      mounted = false;
      if (playerRef.current) {
        playerRef.current.disconnect();
        playerRef.current = null;
      }
      deviceIdRef.current = null;
      deviceActivatedRef.current = false;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const play = useCallback(async (trackId: string): Promise<boolean> => {
    const deviceId = deviceIdRef.current;
    const token = getTokenRef.current();
    if (!deviceId || !token) return false;

    // If the device was never activated, transfer first
    if (!deviceActivatedRef.current) {
      const transferred = await transferPlayback(deviceId, token);
      if (transferred) {
        deviceActivatedRef.current = true;
        // Give Spotify a moment to propagate
        await new Promise((r) => setTimeout(r, 500));
      }
    }

    try {
      const response = await fetch(`${SPOTIFY_API_BASE}/play?device_id=${deviceId}`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ uris: [`spotify:track:${trackId}`] }),
      });

      if (response.ok || response.status === 204) {
        return true;
      }

      // If still 404, try one more transfer + play
      if (response.status === 404) {
        console.log('[SpotifyPlayer] device not found, re-transferring...');
        const transferred = await transferPlayback(deviceId, token);
        if (!transferred) return false;
        deviceActivatedRef.current = true;

        await new Promise((r) => setTimeout(r, 1000));

        const retry = await fetch(`${SPOTIFY_API_BASE}/play?device_id=${deviceId}`, {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ uris: [`spotify:track:${trackId}`] }),
        });
        return retry.ok || retry.status === 204;
      }

      if (response.status === 401 || response.status === 403) {
        return false;
      }

      console.warn('[SpotifyPlayer] play failed:', response.status);
      return false;
    } catch {
      return false;
    }
  }, []);

  const pause = useCallback(async () => {
    await playerRef.current?.pause();
  }, []);

  const resume = useCallback(async () => {
    await playerRef.current?.resume();
  }, []);

  const disconnect = useCallback(() => {
    playerRef.current?.disconnect();
    playerRef.current = null;
    deviceIdRef.current = null;
    deviceActivatedRef.current = false;
    setIsReady(false);
    setIsPlaying(false);
  }, []);

  return { isReady, isPlaying, play, pause, resume, disconnect };
}

/**
 * Transfers playback to our SDK device so Spotify's API recognizes it.
 * `play: false` keeps it silent.
 */
async function transferPlayback(deviceId: string, token: string): Promise<boolean> {
  try {
    const response = await fetch(SPOTIFY_API_BASE, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ device_ids: [deviceId], play: false }),
    });
    return response.ok || response.status === 204;
  } catch {
    return false;
  }
}
