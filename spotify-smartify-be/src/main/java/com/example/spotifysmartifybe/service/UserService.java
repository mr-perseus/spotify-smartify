package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final SpotifyApiFactory spotifyApiFactory;

    public User getCurrentUserProfile(String accessToken) throws UnauthorizedException {
        try {
            return spotifyApiFactory.createWithAccessToken(accessToken)
                    .getCurrentUsersProfile()
                    .build()
                    .execute();
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access to user profile", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch user profile", e);
            throw new SpotifyApiException("Failed to fetch user profile", e);
        }
    }

    public List<Track> getTopTracks(String accessToken, String timeRange) throws UnauthorizedException {
        try {
            Track[] items = spotifyApiFactory.createWithAccessToken(accessToken)
                    .getUsersTopTracks()
                    .limit(50)
                    .time_range(timeRange)
                    .build()
                    .execute()
                    .getItems();
            return items != null ? Arrays.asList(items) : List.of();
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access to top tracks", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch top tracks", e);
            throw new SpotifyApiException("Failed to fetch top tracks", e);
        }
    }
}
