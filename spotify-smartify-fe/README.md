# spotify-smartify-fe

React + TypeScript frontend for Spotify Smartify.

## Prerequisites

- Node.js 18+
- The backend (`spotify-smartify-be`) must be running on `http://127.0.0.1:8080`

## Setup

```bash
npm install
```

## Running

```bash
npm start
```

The app starts on `http://127.0.0.1:3000`. The `start` script explicitly binds to `127.0.0.1` (not `localhost`) because Spotify's OAuth redirect URI allowlist only accepts `127.0.0.1` for local development.

## Configuration

The backend URL is set via the `REACT_APP_API_BASE_URL` environment variable in `.env`:

```
REACT_APP_API_BASE_URL=http://127.0.0.1:8080
```

Override this (e.g. in `.env.local`) when pointing at a different backend.

## Pages

| Route       | Description                                                                          |
|-------------|--------------------------------------------------------------------------------------|
| `/`         | Login page — initiates Spotify OAuth flow                                            |
| `/callback` | Receives tokens from the backend redirect, saves them, and navigates to `/profile`   |
| `/profile`  | Main page — select time range, enter your name, view top 50 tracks with 30s previews |

## Running tests

```bash
npm test
```
