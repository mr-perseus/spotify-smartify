package com.example.spotifysmartifybe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SpotifyApi spotifyApi;

    /**
     * Builds the Spotify authorization URL the user should be redirected to.
     * Scopes can be expanded as needed.
     */
    public URI getAuthorizationUri() {
        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope("user-read-private user-read-email user-top-read playlist-read-private")
                .show_dialog(true)
                .build();
        return request.execute();
    }

    /**
     * Exchanges the authorization code (received in the callback) for access
     * and refresh tokens. Stores them back on the SpotifyApi instance so that
     * subsequent API calls are authenticated.
     *
     * @param code the authorization code returned by Spotify
     * @return the credentials containing access token, refresh token and expiry
     */
    public AuthorizationCodeCredentials exchangeCode(String code) {
        try {
            AuthorizationCodeRequest request = spotifyApi.authorizationCode(code).build();
            AuthorizationCodeCredentials credentials = request.execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
            return credentials;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange Spotify authorization code", e);
        }
    }
}
