package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SCOPES = "user-read-private user-read-email user-top-read";

    private final SpotifyApiFactory spotifyApiFactory;

    public URI getAuthorizationUri() {
        return spotifyApiFactory.createForAuth()
                .authorizationCodeUri()
                .scope(SCOPES)
                .show_dialog(true)
                .build()
                .execute();
    }

    public AuthorizationCodeCredentials exchangeCode(String code) {
        try {
            return spotifyApiFactory.createForAuth()
                    .authorizationCode(code)
                    .build()
                    .execute();
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to exchange Spotify authorization code", e);
        }
    }

    public AuthorizationCodeCredentials refreshAccessToken(String refreshToken) {
        try {
            return spotifyApiFactory.createWithRefreshToken(refreshToken)
                    .authorizationCodeRefresh()
                    .build()
                    .execute();
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to refresh access token", e);
        }
    }
}
