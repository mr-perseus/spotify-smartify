import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../services/authApi';
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

    const code = searchParams.get('code');
    const error = searchParams.get('error');

    if (error || !code) {
      navigate(`/?error=${encodeURIComponent(error || 'missing_code')}`, { replace: true });
      return;
    }

    authApi.exchangeCode(code)
      .then((tokens) => {
        saveTokens(tokens);
        navigate('/home', { replace: true });
      })
      .catch((e: Error) => {
        navigate(`/?error=${encodeURIComponent(e.message)}`, { replace: true });
      });
  }, [searchParams, navigate, saveTokens]);

  return (
    <div className="callback-page">
      <div className="callback-spinner" />
      <p className="callback-text">Completing login...</p>
    </div>
  );
}
