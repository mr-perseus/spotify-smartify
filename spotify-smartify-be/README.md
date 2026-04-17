# spotify-smartify-be

Spring Boot backend for Spotify Smartify. Implements the Spotify Authorization Code Flow and exposes auth endpoints consumed by the React frontend.

## Environment Variables

The application requires the following environment variables to be set before starting:

| Variable                | Required | Description                                                           |
|-------------------------|----------|-----------------------------------------------------------------------|
| `SPOTIFY_CLIENT_ID`     | Yes      | Your Spotify application's Client ID                                  |
| `SPOTIFY_CLIENT_SECRET` | Yes      | Your Spotify application's Client Secret                              |
| `SPOTIFY_REDIRECT_URI`  | No       | OAuth callback URI. Defaults to `http://127.0.0.1:8080/auth/callback` |
| `ALLOWED_SPOTIFY_IDS`   | No       | Comma-separated list of Spotify user IDs allowed to log in. If not set, all users are allowed |
| `FRONTEND_URL`          | No       | Base URL of the React frontend. Defaults to `http://127.0.0.1:3000`  |

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

| Method | Path                         | Description                                                    |
|--------|------------------------------|----------------------------------------------------------------|
| `GET`  | `/auth/login`                | Returns the Spotify authorization URL to redirect the user to  |
| `GET`  | `/auth/callback?code=<code>` | Exchanges the authorization code for access and refresh tokens |
