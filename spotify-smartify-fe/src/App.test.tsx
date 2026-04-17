import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

test('renders login page when not authenticated', () => {
  render(<App />);
  expect(screen.getByRole('button', { name: /login with spotify/i })).toBeInTheDocument();
});
