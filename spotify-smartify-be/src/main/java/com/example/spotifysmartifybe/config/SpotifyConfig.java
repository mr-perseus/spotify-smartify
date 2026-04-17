package com.example.spotifysmartifybe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

@Configuration
public class SpotifyConfig {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    @Bean
    public SpotifyApi spotifyApi() {
        return SpotifyApi.builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(SpotifyHttpManager.makeUri(redirectUri))
                .build();
    }
}
