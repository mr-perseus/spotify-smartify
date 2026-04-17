import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi';
import { UnauthorizedError } from '../services/errors';
import './DashboardPage.css';

const SpotifyLogo = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" width="36" height="36">
    <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
  </svg>
);

const FEATURES = [
  {
    key: 'top-tracks',
    title: 'Top Tracks',
    description: 'See your most-played songs across different time periods.',
    path: '/profile',
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
        <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z" />
      </svg>
    ),
  },
  {
    key: 'playlist-quiz',
    title: 'Playlist Quiz',
    description: 'Load a playlist, listen to tracks, and guess the song.',
    path: '/playlist',
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
        <path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z" />
      </svg>
    ),
  },
] as const;

export default function DashboardPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const api = useAuthenticatedApi();

  const [displayName, setDisplayName] = useState<string | null>(null);

  useEffect(() => {
    api.getProfile()
      .then((profile) => {
        if (profile.displayName) setDisplayName(profile.displayName);
      })
      .catch((err) => {
        if (err instanceof UnauthorizedError) logout();
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="dashboard-page">
      <div className="dashboard-card">
        {/* Header */}
        <div className="dashboard-header">
          <div className="dashboard-logo"><SpotifyLogo /></div>
          <div className="dashboard-header-text">
            <h1 className="dashboard-title">Spotify Smartify</h1>
            {displayName && (
              <p className="dashboard-greeting">Welcome back, {displayName}</p>
            )}
          </div>
          <button className="dashboard-logout" onClick={logout}>Log out</button>
        </div>

        {/* Feature cards */}
        <div className="dashboard-features">
          {FEATURES.map((feature) => (
            <button
              key={feature.key}
              className="feature-card"
              onClick={() => navigate(feature.path)}
            >
              <div className="feature-icon">{feature.icon}</div>
              <div className="feature-text">
                <h2 className="feature-title">{feature.title}</h2>
                <p className="feature-description">{feature.description}</p>
              </div>
              <svg className="feature-arrow" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
              </svg>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
