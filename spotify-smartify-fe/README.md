# spotify-smartify-fe

React + TypeScript frontend for Spotify Smartify.

## Prerequisites

- Node.js 22+
- pnpm (`corepack enable` or `npm install -g pnpm`)
- The backend (`spotify-smartify-be`) must be running on `http://127.0.0.1:8080`

## Setup

```bash
pnpm install
```

## Running

```bash
pnpm start
```

The app starts on `http://127.0.0.1:3000`. The dev server binds to `127.0.0.1` (not `localhost`) via `HOST=127.0.0.1` in `.env.development`, because Spotify's OAuth redirect URI allowlist only accepts `127.0.0.1` for local development.

## Configuration

The backend URL is set via the `REACT_APP_API_BASE_URL` environment variable in `.env.development`:

```
REACT_APP_API_BASE_URL=http://127.0.0.1:8080
```

Override this (e.g. in `.env.local`) when pointing at a different backend. For production deployments, set `REACT_APP_API_BASE_URL` as an environment variable in your hosting provider — see `.env.example` for reference.

## Pages

| Route        | Description                                                                                        |
|--------------|----------------------------------------------------------------------------------------------------|
| `/`          | Login page — initiates Spotify OAuth flow. Redirects to `/dashboard` if already authenticated      |
| `/callback`  | Receives tokens from the backend redirect, saves them, and navigates to `/dashboard`               |
| `/dashboard` | Main hub — shows available features (top tracks, playlist quiz)                                    |
| `/profile`   | Top tracks page — select time range, enter your name, view top 50 tracks with 30s previews         |
| `/playlist`  | Playlist quiz — enter a playlist URL, guess which tracks belong to it                              |

## Running tests

```bash
pnpm test
```
