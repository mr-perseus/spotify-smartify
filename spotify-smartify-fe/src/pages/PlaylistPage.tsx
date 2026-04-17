import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi';
import { useSpotifyPlayer } from '../hooks/useSpotifyPlayer';
import { TopTrack } from '../services/userApi';
import { UnauthorizedError } from '../services/errors';
import './PlaylistPage.css';

function extractPlaylistId(input: string): string | null {
  const trimmed = input.trim();
  const urlMatch = trimmed.match(/spotify\.com\/playlist\/([a-zA-Z0-9]+)/);
  if (urlMatch) return urlMatch[1];
  const uriMatch = trimmed.match(/spotify:playlist:([a-zA-Z0-9]+)/);
  if (uriMatch) return uriMatch[1];
  if (/^[a-zA-Z0-9]{10,}$/.test(trimmed)) return trimmed;
  return null;
}

function shuffled<T>(arr: T[]): T[] {
  const result = [...arr];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

const SpotifyLogo = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
    <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
  </svg>
);

const EyeIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
    strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

const EyeOffIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
    strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
    <line x1="1" y1="1" x2="23" y2="23" />
  </svg>
);

/** Which audio source is currently active */
type PlaybackMode = 'spotify' | 'preview' | 'none';

export default function PlaylistPage() {
  const navigate = useNavigate();
  const { tokens, logout } = useAuth();
  const api = useAuthenticatedApi();

  // Spotify Web Playback SDK
  const spotify = useSpotifyPlayer(
    useCallback(() => tokens?.accessToken ?? null, [tokens]),
  );

  // Input phase
  const [urlInput, setUrlInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Game phase
  const [playlistName, setPlaylistName] = useState('');
  const [tracks, setTracks] = useState<TopTrack[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);

  // Audio state
  const [playbackMode, setPlaybackMode] = useState<PlaybackMode>('none');
  const [previewPlaying, setPreviewPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Derived: "is anything playing?"
  const isPlaying = playbackMode === 'spotify' ? spotify.isPlaying : previewPlaying;

  // Stop preview audio
  const stopPreview = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.onended = null;
      audioRef.current = null;
    }
    setPreviewPlaying(false);
  }, []);

  // Stop ALL audio (SDK + preview)
  const stopAll = useCallback(async () => {
    stopPreview();
    if (spotify.isPlaying) {
      await spotify.pause();
    }
    setPlaybackMode('none');
  }, [stopPreview, spotify]);

  // Cleanup on unmount
  useEffect(() => () => {
    audioRef.current?.pause();
    spotify.disconnect();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Play a track. Tries Spotify SDK first, falls back to 30s preview.
   */
  const playTrack = useCallback(async (track: TopTrack) => {
    // Stop whatever was playing before
    stopPreview();
    if (spotify.isPlaying) {
      await spotify.pause();
    }

    // Try full Spotify playback
    if (spotify.isReady && track.id) {
      const started = await spotify.play(track.id);
      if (started) {
        setPlaybackMode('spotify');
        return;
      }
    }

    // Fallback: 30s preview
    if (!track.previewUrl) {
      setPlaybackMode('none');
      return;
    }
    const audio = new Audio(track.previewUrl);
    audioRef.current = audio;
    audio.onended = () => {
      setPreviewPlaying(false);
      setPlaybackMode('none');
    };
    audio.play()
      .then(() => {
        setPreviewPlaying(true);
        setPlaybackMode('preview');
      })
      .catch(() => {
        setPreviewPlaying(false);
        setPlaybackMode('none');
      });
  }, [spotify, stopPreview]);

  const handleLoad = async (e: React.FormEvent) => {
    e.preventDefault();
    const playlistId = extractPlaylistId(urlInput);
    if (!playlistId) {
      setLoadError('Could not find a playlist ID. Paste a Spotify playlist URL or URI.');
      return;
    }
    await stopAll();
    setLoading(true);
    setLoadError(null);
    try {
      const result = await api.getPlaylistTracks(playlistId);
      if (result.tracks.length === 0) {
        setLoadError('This playlist has no playable tracks.');
        return;
      }
      const s = shuffled(result.tracks);
      setPlaylistName(result.playlistName);
      setTracks(s);
      setCurrentIndex(0);
      setRevealed(false);
      playTrack(s[0]);
    } catch (err: any) {
      if (err instanceof UnauthorizedError) {
        logout();
      } else {
        setLoadError(err.message || 'Could not load the playlist. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const goToIndex = async (index: number) => {
    const track = tracks[index];
    setCurrentIndex(index);
    setRevealed(false);
    playTrack(track);
  };

  const handleShuffle = async () => {
    const s = shuffled(tracks);
    setTracks(s);
    setCurrentIndex(0);
    setRevealed(false);
    playTrack(s[0]);
  };

  const togglePlay = async () => {
    const track = tracks[currentIndex];
    if (!track) return;

    if (isPlaying) {
      // Pause whatever is playing
      if (playbackMode === 'spotify') {
        await spotify.pause();
      } else {
        stopPreview();
        setPlaybackMode('none');
      }
    } else {
      // Resume or start
      if (playbackMode === 'spotify') {
        await spotify.resume();
      } else {
        // Start fresh playback
        playTrack(track);
      }
    }
  };

  const currentTrack = tracks[currentIndex];
  const isFirst = currentIndex === 0;
  const isLast = currentIndex === tracks.length - 1;
  const hasAudio = spotify.isReady || !!currentTrack?.previewUrl;

  // ── Input phase ──────────────────────────────────────────────────────────
  if (tracks.length === 0) {
    return (
      <div className="playlist-page">
        <div className="playlist-card">
          <Header
            onBack={() => navigate('/profile')}
            onLogout={logout}
            sdkReady={spotify.isReady}
          />
          <form className="pl-form" onSubmit={handleLoad}>
            <label className="pl-label" htmlFor="pl-url">
              Paste a Spotify playlist link
            </label>
            <div className="pl-input-row">
              <input
                id="pl-url"
                type="text"
                className="pl-input"
                placeholder="https://open.spotify.com/playlist/..."
                value={urlInput}
                onChange={e => setUrlInput(e.target.value)}
                disabled={loading}
              />
              <button
                type="submit"
                className="pl-load-btn"
                disabled={!urlInput.trim() || loading}
              >
                {loading ? 'Loading...' : 'Load'}
              </button>
            </div>
            {loadError && <p className="pl-error">{loadError}</p>}
          </form>
        </div>
      </div>
    );
  }

  // ── Game phase ────────────────────────────────────────────────────────────
  return (
    <div className="playlist-page">
      <div className="playlist-card">
        <Header
          subtitle={playlistName}
          onBack={async () => { await stopAll(); navigate('/profile'); }}
          onLogout={logout}
          sdkReady={spotify.isReady}
        />

        <p className="game-counter">
          Track {currentIndex + 1} of {tracks.length}
        </p>

        {/* Album art */}
        <div className="game-art-wrap">
          {currentTrack.albumImageUrl ? (
            <img
              key={currentTrack.id}
              className="game-art"
              src={currentTrack.albumImageUrl}
              alt=""
            />
          ) : (
            <div className="game-art-placeholder">
              <svg viewBox="0 0 24 24" fill="currentColor" width="48" height="48" opacity="0.3">
                <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z" />
              </svg>
            </div>
          )}
        </div>

        {/* Hidden / revealed info */}
        <div className="game-info-row">
          <div className="game-info-text">
            <p className={`game-track-name${revealed ? '' : ' blurred'}`}>
              {currentTrack.name}
            </p>
            <p className={`game-track-artist${revealed ? '' : ' blurred'}`}>
              {currentTrack.artists}
            </p>
          </div>
          <button
            className={`game-eye-btn${revealed ? ' active' : ''}`}
            type="button"
            onClick={() => setRevealed(r => !r)}
            title={revealed ? 'Hide info' : 'Reveal title and artist'}
          >
            {revealed ? <EyeOffIcon /> : <EyeIcon />}
          </button>
        </div>

        {/* Play / pause */}
        <div className="game-play-row">
          {hasAudio ? (
            <>
              <button
                className={`game-play-btn${isPlaying ? ' playing' : ''}`}
                type="button"
                onClick={togglePlay}
              >
                {isPlaying ? (
                  <>
                    <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" aria-hidden="true">
                      <rect x="6" y="4" width="4" height="16" rx="1" />
                      <rect x="14" y="4" width="4" height="16" rx="1" />
                    </svg>
                    Pause
                  </>
                ) : (
                  <>
                    <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" aria-hidden="true">
                      <path d="M8 5v14l11-7z" />
                    </svg>
                    Play
                  </>
                )}
              </button>
              {playbackMode !== 'none' && (
                <span className="game-playback-badge">
                  {playbackMode === 'spotify' ? 'Spotify' : 'Preview'}
                </span>
              )}
            </>
          ) : (
            <p className="game-no-preview">No audio available</p>
          )}
        </div>

        {/* Navigation */}
        <div className="game-nav">
          <button
            className="game-nav-btn"
            type="button"
            disabled={isFirst}
            onClick={() => goToIndex(currentIndex - 1)}
          >
            Prev
          </button>
          <button
            className="game-shuffle-btn"
            type="button"
            onClick={handleShuffle}
            title="Shuffle and restart"
          >
            Shuffle
          </button>
          <button
            className="game-nav-btn"
            type="button"
            disabled={isLast}
            onClick={() => goToIndex(currentIndex + 1)}
          >
            Next
          </button>
        </div>

        {isLast && (
          <p className="game-finished">
            You've heard all {tracks.length} tracks — hit Shuffle to play again!
          </p>
        )}
      </div>
    </div>
  );
}

function Header({
  subtitle,
  onBack,
  onLogout,
  sdkReady,
}: {
  subtitle?: string;
  onBack: () => void;
  onLogout: () => void;
  sdkReady: boolean;
}) {
  return (
    <div className="pl-header">
      <div className="pl-logo"><SpotifyLogo /></div>
      <div className="pl-header-text">
        <h1 className="pl-title">Playlist Quiz</h1>
        {subtitle && <p className="pl-subtitle">{subtitle}</p>}
      </div>
      <span className={`pl-sdk-badge ${sdkReady ? 'ready' : 'not-ready'}`}>
        {sdkReady ? 'Full playback' : 'Preview only'}
      </span>
      <button className="pl-back-btn" onClick={onBack}>Top tracks</button>
      <button className="pl-logout-btn" onClick={onLogout}>Log out</button>
    </div>
  );
}
