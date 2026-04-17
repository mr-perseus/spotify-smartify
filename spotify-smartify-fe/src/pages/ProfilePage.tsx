import React, { useEffect, useState, FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi, TopTrack } from '../services/userApi';
import './ProfilePage.css';

export default function ProfilePage() {
  const { logout } = useAuth();

  const [username, setUsername] = useState('');
  const [savedUsername, setSavedUsername] = useState('');
  const [topTrack, setTopTrack] = useState<TopTrack | null>(null);
  const [loadingTrack, setLoadingTrack] = useState(false);
  const [trackError, setTrackError] = useState<string | null>(null);

  // Pre-fill username from Spotify profile on first load
  useEffect(() => {
    userApi.getProfile()
      .then((profile) => {
        if (profile.displayName) {
          setUsername(profile.displayName);
        }
      })
      .catch(() => {
        // non-critical — user can type manually
      });
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim()) return;

    setSavedUsername(username.trim());
    setLoadingTrack(true);
    setTrackError(null);
    setTopTrack(null);

    try {
      const track = await userApi.getTopTrack();
      setTopTrack(track);
    } catch (err: any) {
      setTrackError(err.message || 'Could not load top track.');
    } finally {
      setLoadingTrack(false);
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-card">
        {/* Header */}
        <div className="profile-header">
          <div className="profile-logo">
            <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
              <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
            </svg>
          </div>
          <h1 className="profile-title">Spotify Smartify</h1>
          <button className="profile-logout" onClick={logout} title="Log out">
            Log out
          </button>
        </div>

        {/* Username form */}
        <form className="profile-form" onSubmit={handleSubmit}>
          <label className="profile-label" htmlFor="username">
            Your name
          </label>
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
              disabled={!username.trim() || loadingTrack}
            >
              {loadingTrack ? 'Loading…' : 'Show my top track'}
            </button>
          </div>
        </form>

        {/* Top track result */}
        {savedUsername && (
          <div className="profile-result">
            {loadingTrack && (
              <div className="profile-loading">
                <div className="profile-spinner" />
                <p>Fetching {savedUsername}'s top track…</p>
              </div>
            )}

            {trackError && !loadingTrack && (
              <p className="profile-error">{trackError}</p>
            )}

            {topTrack && !loadingTrack && (
              <div className="top-track">
                <p className="top-track-heading">
                  {savedUsername}'s most listened song
                </p>
                <div className="top-track-card">
                  {topTrack.albumImageUrl && (
                    <img
                      className="top-track-art"
                      src={topTrack.albumImageUrl}
                      alt={`${topTrack.albumName} cover`}
                    />
                  )}
                  <div className="top-track-info">
                    <p className="top-track-name">{topTrack.name}</p>
                    <p className="top-track-artists">{topTrack.artists}</p>
                    <p className="top-track-album">{topTrack.albumName}</p>
                    {topTrack.spotifyUrl && (
                      <a
                        className="top-track-link"
                        href={topTrack.spotifyUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Open in Spotify
                      </a>
                    )}
                  </div>
                </div>
                {topTrack.previewUrl && (
                  <audio
                    className="top-track-audio"
                    controls
                    src={topTrack.previewUrl}
                  />
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
