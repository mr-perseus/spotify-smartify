package com.example.spotifysmartifybe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SpotifyApi spotifyApi;

    public User getCurrentUserProfile() {
        try {
            GetCurrentUsersProfileRequest request = spotifyApi.getCurrentUsersProfile().build();
            return request.execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user profile", e);
        }
    }

    public Track getTopTrack() {
        try {
            GetUsersTopTracksRequest request = spotifyApi.getUsersTopTracks()
                    .limit(1)
                    .time_range("medium_term")
                    .build();
            var paging = request.execute();
            if (paging.getItems().length == 0) {
                throw new RuntimeException("No top tracks found");
            }
            return paging.getItems()[0];
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch top track", e);
        }
    }
}
