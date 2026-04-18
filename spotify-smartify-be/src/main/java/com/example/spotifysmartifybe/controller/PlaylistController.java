package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.spotifysmartifybe.controller.BearerTokenResolver.requireBearerToken;

@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistResponse> getPlaylistTracks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String playlistId) {

        String accessToken = requireBearerToken(authHeader);
        PlaylistResponse response = playlistService.getPlaylistWithTracks(accessToken, playlistId);
        return ResponseEntity.ok(response);
    }
}
