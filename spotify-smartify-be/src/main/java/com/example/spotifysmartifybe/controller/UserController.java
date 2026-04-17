package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> VALID_TIME_RANGES = Set.of("short_term", "medium_term", "long_term");

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader)
            throws UnauthorizedException {
        String accessToken = requireBearerToken(authHeader);
        User user = userService.getCurrentUserProfile(accessToken);
        return ResponseEntity.ok(Map.of(
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "email", user.getEmail() != null ? user.getEmail() : ""
        ));
    }

    @GetMapping("/top-tracks")
    public ResponseEntity<List<TrackResponse>> getTopTracks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "medium_term") String timeRange)
            throws UnauthorizedException {
        String accessToken = requireBearerToken(authHeader);
        if (!VALID_TIME_RANGES.contains(timeRange)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid timeRange. Must be one of: short_term, medium_term, long_term");
        }
        List<TrackResponse> tracks = userService.getTopTracks(accessToken, timeRange).stream()
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

    private String requireBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
