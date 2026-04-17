import React, { useEffect, useState, useRef, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi';
import { TopTrack, TimeRange, TIME_RANGE_LABELS } from '../services/userApi';
import { UnauthorizedError } from '../services/errors';
import './ProfilePage.css';

export default function ProfilePage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const api = useAuthenticatedApi();

  const [username, setUsername] = useState('');
  const [savedUsername, setSavedUsername] = useState('');
  const [profileError, setProfileError] = useState<string | null>(null);
  const [timeRange, setTimeRange] = useState<TimeRange>('medium_term');
  const [tracks, setTracks] = useState<TopTrack[]>([]);
  const [loadingTracks, setLoadingTracks] = useState(false);
  const [tracksError, setTracksError] = useState<string | null>(null);

  // Audio preview state
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [playingId, setPlayingId] = useState<string | null>(null);

  // Pre-fill username from Spotify profile on mount
  useEffect(() => {
    api.getProfile()
      .then((profile) => {
        if (profile.displayName) setUsername(profile.displayName);
      })
      .catch((err) => {
        if (err instanceof UnauthorizedError) {
          logout();
        } else {
          setProfileError('Could not load your Spotify profile. Please try again.');
        }
      });
    // api is intentionally excluded: profile fetch should only run once on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Stop playback whenever the track list is replaced
  useEffect(() => {
    audioRef.current?.pause();
    setPlayingId(null);
  }, [tracks]);

  const togglePreview = (track: TopTrack) => {
    if (!track.previewUrl) return;
    if (playingId === track.id) {
      audioRef.current?.pause();
      setPlayingId(null);
    } else {
      audioRef.current?.pause();
      audioRef.current = new Audio(track.previewUrl);
      audioRef.current.play();
      audioRef.current.onended = () => setPlayingId(null);
      setPlayingId(track.id);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim()) return;

    setSavedUsername(username.trim());
    setLoadingTracks(true);
    setTracksError(null);
    setTracks([]);

    try {
      const data = await api.getTopTracks(timeRange);
      setTracks(data);
    } catch (err: any) {
      if (err instanceof UnauthorizedError) {
        logout();
      } else {
        setTracksError(err.message || 'Could not load top tracks.');
      }
    } finally {
      setLoadingTracks(false);
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-card">

        {/* Header */}
        <div className="profile-header">
          <button className="profile-back-btn" onClick={() => navigate('/dashboard')}>
            <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
            </svg>
            Dashboard
          </button>
          <h1 className="profile-title">Top Tracks</h1>
          <button className="profile-logout" onClick={logout}>Log out</button>
        </div>

        {/* Profile error */}
        {profileError && <p className="profile-error">{profileError}</p>}

        {/* Time range selector */}
        <div className="time-range-selector">
          {(Object.keys(TIME_RANGE_LABELS) as TimeRange[]).map((range) => (
            <button
              key={range}
              type="button"
              className={`time-range-btn${timeRange === range ? ' active' : ''}`}
              onClick={() => setTimeRange(range)}
            >
              {TIME_RANGE_LABELS[range]}
            </button>
          ))}
        </div>

        {/* Username form */}
        <form className="profile-form" onSubmit={handleSubmit}>
          <label className="profile-label" htmlFor="username">Your name</label>
          <div className="profile-input-row">
            <input
              id="username"
              className="profile-input"
              type="text"
              placeholder="Enter your name"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              maxLength={50}
            />
            <button
              className="profile-submit"
              type="submit"
              disabled={!username.trim() || loadingTracks}
            >
              {loadingTracks ? 'Loading…' : 'Show my top tracks'}
            </button>
          </div>
        </form>

        {/* Results */}
        {savedUsername && (
          <div className="profile-result">
            {loadingTracks && (
              <div className="profile-loading">
                <div className="profile-spinner" />
                <p>Fetching {savedUsername}'s top tracks…</p>
              </div>
            )}

            {tracksError && !loadingTracks && (
              <p className="profile-error">{tracksError}</p>
            )}

            {tracks.length > 0 && !loadingTracks && (
              <>
                <p className="tracks-heading">
                  {savedUsername}'s top {tracks.length} tracks
                  &nbsp;·&nbsp;
                  {TIME_RANGE_LABELS[timeRange].toLowerCase()}
                </p>
                <div className="tracks-list">
                  {tracks.map((track, index) => (
                    <div key={track.id} className="track-row">
                      <span className="track-rank">#{index + 1}</span>
                      {track.albumImageUrl && (
                        <img
                          className="track-art"
                          src={track.albumImageUrl}
                          alt={track.albumName}
                        />
                      )}
                      <div className="track-info">
                        <a
                          className="track-name"
                          href={track.spotifyUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {track.name}
                        </a>
                        <p className="track-artists">{track.artists}</p>
                        <p className="track-album">{track.albumName}</p>
                      </div>
                      {track.previewUrl ? (
                        <button
                          className={`track-preview-btn${playingId === track.id ? ' playing' : ''}`}
                          type="button"
                          onClick={() => togglePreview(track)}
                          title={playingId === track.id ? 'Pause preview' : 'Play 30s preview'}
                        >
                          {playingId === track.id ? (
                            <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" aria-hidden="true">
                              <rect x="6" y="4" width="4" height="16" rx="1" />
                              <rect x="14" y="4" width="4" height="16" rx="1" />
                            </svg>
                          ) : (
                            <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" aria-hidden="true">
                              <path d="M8 5v14l11-7z" />
                            </svg>
                          )}
                        </button>
                      ) : (
                        <span className="track-preview-btn no-preview" title="No preview available" />
                      )}
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
