package com.example.spotifysmartifybe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

@Component
public class SpotifyApiFactory {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    /** Creates a SpotifyApi instance for OAuth operations (auth URL, code exchange). No user tokens. */
    public SpotifyApi createForAuth() {
        return SpotifyApi.builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(SpotifyHttpManager.makeUri(redirectUri))
                .build();
    }

    /** Creates a per-request SpotifyApi instance for user-specific API calls. */
    public SpotifyApi createWithAccessToken(String accessToken) {
        return SpotifyApi.builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setAccessToken(accessToken)
                .build();
    }

    /** Creates a SpotifyApi instance for token refresh operations. */
    public SpotifyApi createWithRefreshToken(String refreshToken) {
        return SpotifyApi.builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();
    }
}
