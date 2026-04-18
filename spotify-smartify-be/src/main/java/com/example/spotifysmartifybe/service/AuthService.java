package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String SCOPES =
            "user-read-private user-read-email user-top-read " +
            "playlist-read-private playlist-read-collaborative " +
            "streaming user-modify-playback-state";

    private final SpotifyApiFactory spotifyApiFactory;

    public URI getAuthorizationUri() {
        return spotifyApiFactory.createForAuth()
                .authorizationCodeUri()
                .scope(SCOPES)
                .show_dialog(true)
                .build()
                .execute();
    }

    public AuthorizationCodeCredentials exchangeCode(String code) throws TooManyRequestsException {
        try {
            return spotifyApiFactory.createForAuth()
                    .authorizationCode(code)
                    .build()
                    .execute();
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to exchange Spotify authorization code", e);
            throw new SpotifyApiException("Failed to exchange Spotify authorization code", e);
        }
    }

    public AuthorizationCodeCredentials refreshAccessToken(String refreshToken) throws TooManyRequestsException {
        try {
            return spotifyApiFactory.createWithRefreshToken(refreshToken)
                    .authorizationCodeRefresh()
                    .build()
                    .execute();
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refresh access token", e);
            throw new SpotifyApiException("Failed to refresh access token", e);
        }
    }
}
