package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile() throws UnauthorizedException {
        User user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(Map.of(
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "email", user.getEmail() != null ? user.getEmail() : ""
        ));
    }

    @GetMapping("/top-tracks")
    public ResponseEntity<List<TrackResponse>> getTopTracks() throws UnauthorizedException {
        List<TrackResponse> tracks = userService.getTopTracks().stream()
                .map(track -> new TrackResponse(
                        track.getId(),
                        track.getName(),
                        Arrays.stream(track.getArtists())
                                .map(ArtistSimplified::getName)
                                .collect(java.util.stream.Collectors.joining(", ")),
                        track.getAlbum().getName(),
                        track.getAlbum().getImages().length > 0
                                ? track.getAlbum().getImages()[0].getUrl()
                                : "",
                        track.getPreviewUrl() != null ? track.getPreviewUrl() : "",
                        track.getExternalUrls().get("spotify")
                ))
                .toList();

        return ResponseEntity.ok(tracks);
    }
}
