package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.List;
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
    public ResponseEntity<Map<String, String>> getProfile() throws UnauthorizedException {
        User user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(Map.of(
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "email", user.getEmail() != null ? user.getEmail() : ""
        ));
    }

    /**
     * GET /user/top-tracks
     * Returns the authenticated user's top 50 tracks (medium-term).
     */
    @GetMapping("/top-tracks")
    public ResponseEntity<List<Map<String, String>>> getTopTracks() throws UnauthorizedException {
        List<Track> tracks = userService.getTopTracks();

        List<Map<String, String>> result = tracks.stream()
                .map(track -> {
                    String artists = Arrays.stream(track.getArtists())
                            .map(ArtistSimplified::getName)
                            .collect(Collectors.joining(", "));

                    String albumImageUrl = (track.getAlbum().getImages().length > 0)
                            ? track.getAlbum().getImages()[0].getUrl()
                            : "";

                    return Map.of(
                            "id", track.getId(),
                            "name", track.getName(),
                            "artists", artists,
                            "albumName", track.getAlbum().getName(),
                            "albumImageUrl", albumImageUrl,
                            "previewUrl", track.getPreviewUrl() != null ? track.getPreviewUrl() : "",
                            "spotifyUrl", track.getExternalUrls().get("spotify")
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
