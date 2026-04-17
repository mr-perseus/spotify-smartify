package com.example.spotifysmartifybe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SpotifyApi spotifyApi;

    public User getCurrentUserProfile() throws UnauthorizedException {
        try {
            GetCurrentUsersProfileRequest request = spotifyApi.getCurrentUsersProfile().build();
            return request.execute();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile", e);
        }
    }

    public List<Track> getTopTracks() throws UnauthorizedException {
        try {
            GetUsersTopTracksRequest request = spotifyApi.getUsersTopTracks()
                    .limit(50)
                    .time_range("medium_term")
                    .build();
            Track[] items = request.execute().getItems();
            if (items.length == 0) {
                throw new RuntimeException("No top tracks found");
            }
            return Arrays.asList(items);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch top tracks", e);
        }
    }
}
