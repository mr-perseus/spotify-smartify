# spotify-smartify-be

Spring Boot backend for Spotify Smartify. Implements the Spotify Authorization Code Flow and exposes auth endpoints consumed by the React frontend.

## Environment Variables

The application requires the following environment variables to be set before starting:

| Variable                | Required | Description                                                                                   |
|-------------------------|----------|-----------------------------------------------------------------------------------------------|
| `SPOTIFY_CLIENT_ID`     | Yes      | Your Spotify application's Client ID                                                          |
| `SPOTIFY_CLIENT_SECRET` | Yes      | Your Spotify application's Client Secret                                                      |
| `SPOTIFY_REDIRECT_URI`  | No       | OAuth callback URI. Defaults to `http://127.0.0.1:8080/auth/callback`                         |
| `ALLOWED_SPOTIFY_IDS`   | No       | Comma-separated list of Spotify user IDs allowed to log in. If not set, all users are allowed |
| `FRONTEND_URL`          | No       | Base URL of the React frontend. Defaults to `http://127.0.0.1:3000`                           |

### Where to find your credentials

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Create an app (or open an existing one)
3. Copy the **Client ID** and **Client Secret** from the app overview page
4. Under **Edit Settings**, add the redirect URI (e.g. `http://127.0.0.1:8080/auth/callback`) to the **Redirect URIs** allowlist

### Where to find your Spotify user ID

Your Spotify user ID is needed for `ALLOWED_SPOTIFY_IDS`. To find it:

1. Open [Spotify Web Player](https://open.spotify.com) and log in
2. Click your profile name in the top-right corner → **Profile**
3. The user ID is the string at the end of the profile page URL:
   `https://open.spotify.com/user/`**your_user_id_here**

Alternatively, start the application without `ALLOWED_SPOTIFY_IDS` set, log in, and check the `/user/profile` endpoint — the Spotify ID is visible in the backend logs or can be retrieved from the API.

### Setting environment variables

**macOS / Linux (shell export):**
```bash
export SPOTIFY_CLIENT_ID=your_client_id_here
export SPOTIFY_CLIENT_SECRET=your_client_secret_here
export SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/auth/callback
export ALLOWED_SPOTIFY_IDS=your_spotify_user_id
```

To allow multiple users, separate IDs with commas:
```bash
export ALLOWED_SPOTIFY_IDS=user_id_1,user_id_2
```

**Windows (Command Prompt):**
```cmd
set SPOTIFY_CLIENT_ID=your_client_id_here
set SPOTIFY_CLIENT_SECRET=your_client_secret_here
set SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/auth/callback
set ALLOWED_SPOTIFY_IDS=your_spotify_user_id
```

**IntelliJ IDEA:**  
Open *Run/Debug Configurations* → *Environment variables* and add the key/value pairs there.

**`.env` file (with a tool like `direnv` or `dotenv-java`):**  
The application does not load `.env` files by default. Use one of the methods above or configure your IDE/deployment environment accordingly.

## Running the Application

```bash
./mvnw spring-boot:run
```

The server starts on `http://127.0.0.1:8080`.

## API Endpoints

### Auth endpoints

| Method | Path                         | Auth required | Description                                                                                     |
|--------|------------------------------|---------------|-------------------------------------------------------------------------------------------------|
| `GET`  | `/auth/login`                | No            | Returns `{ authorizationUrl }` — the Spotify OAuth URL to redirect the user to                  |
| `GET`  | `/auth/callback?code=<code>` | No            | Exchanges the code, optionally checks the allowlist, then redirects the browser to the frontend |
| `POST` | `/auth/refresh`              | No            | Accepts `{ refreshToken }` in the request body; returns `{ accessToken, expiresIn }`            |

### User endpoints

All user endpoints require an `Authorization: Bearer <accessToken>` header.

| Method | Path                                 | Description                                                                                                                                               |
|--------|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/user/profile`                      | Returns `{ displayName, email }` for the authenticated user                                                                                               |
| `GET`  | `/user/top-tracks?timeRange=<range>` | Returns a list of up to 50 top tracks. `timeRange` must be `short_term` (last 4 weeks), `medium_term` (last 6 months, default), or `long_term` (all time) |

### Playlist endpoints

All playlist endpoints require an `Authorization: Bearer <accessToken>` header.

| Method | Path                              | Description                                                                                                                                                                 |
|--------|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/playlist/{playlistId}/tracks`   | Returns `{ playlistName, tracks }` for the given playlist. `tracks` is a list of up to 500 track objects `{ id, name, artistName, albumName, albumArtUrl, previewUrl }`. Episodes and local files are excluded. |

`playlistId` is the Spotify playlist ID (e.g. `37i9dQZF1DXcBWIGoYBM5M`). You can also pass a full Spotify playlist URL or URI — the ID must be extracted client-side before calling this endpoint.
