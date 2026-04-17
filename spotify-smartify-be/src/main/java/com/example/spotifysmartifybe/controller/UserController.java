package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /user/profile
     * Returns the authenticated user's Spotify display name and email.
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile() {
        User user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(Map.of(
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "email", user.getEmail() != null ? user.getEmail() : ""
        ));
    }

    /**
     * GET /user/top-track
     * Returns the authenticated user's most listened-to track (medium-term).
     */
    @GetMapping("/top-track")
    public ResponseEntity<Map<String, String>> getTopTrack() {
        Track track = userService.getTopTrack();

        String artists = Arrays.stream(track.getArtists())
                .map(ArtistSimplified::getName)
                .collect(Collectors.joining(", "));

        String albumImageUrl = (track.getAlbum().getImages().length > 0)
                ? track.getAlbum().getImages()[0].getUrl()
                : "";

        return ResponseEntity.ok(Map.of(
                "name", track.getName(),
                "artists", artists,
                "albumName", track.getAlbum().getName(),
                "albumImageUrl", albumImageUrl,
                "previewUrl", track.getPreviewUrl() != null ? track.getPreviewUrl() : "",
                "spotifyUrl", track.getExternalUrls().get("spotify")
        ));
    }
}
