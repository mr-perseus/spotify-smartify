package com.example.spotifysmartifybe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SCOPES = "user-read-private user-read-email user-top-read";

    private final SpotifyApi spotifyApi;

    public URI getAuthorizationUri() {
        return spotifyApi.authorizationCodeUri()
                .scope(SCOPES)
                .show_dialog(true)
                .build()
                .execute();
    }

    public AuthorizationCodeCredentials exchangeCode(String code) {
        try {
            AuthorizationCodeCredentials credentials = spotifyApi.authorizationCode(code)
                    .build()
                    .execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
            return credentials;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange Spotify authorization code", e);
        }
    }

    public void clearTokens() {
        spotifyApi.setAccessToken(null);
        spotifyApi.setRefreshToken(null);
    }
}
