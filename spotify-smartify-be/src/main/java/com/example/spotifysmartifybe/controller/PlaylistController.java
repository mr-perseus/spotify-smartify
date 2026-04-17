package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
public class PlaylistController {

    private static final int BEARER_PREFIX_LENGTH = "Bearer ".length();

    private final PlaylistService playlistService;

    @GetMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistResponse> getPlaylistTracks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String playlistId) {

        String accessToken = requireBearerToken(authHeader);
        PlaylistResponse response = playlistService.getPlaylistWithTracks(accessToken, playlistId);
        return ResponseEntity.ok(response);
    }

    private String requireBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }
        return authHeader.substring(BEARER_PREFIX_LENGTH);
    }
}
