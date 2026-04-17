import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './CallbackPage.css';

export default function CallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { saveTokens } = useAuth();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const error = searchParams.get('error');
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');
    const expiresIn = searchParams.get('expiresIn');

    if (error || !accessToken || !refreshToken || !expiresIn) {
      navigate(`/?error=${encodeURIComponent(error || 'missing_tokens')}`, { replace: true });
      return;
    }

    saveTokens({
      accessToken,
      refreshToken,
      expiresIn: parseInt(expiresIn, 10),
    });

    navigate('/profile', { replace: true });
  }, [searchParams, navigate, saveTokens]);

  return (
    <div className="callback-page">
      <div className="callback-spinner" />
      <p className="callback-text">Completing login...</p>
    </div>
  );
}
