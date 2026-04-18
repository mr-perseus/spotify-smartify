package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import com.example.spotifysmartifybe.service.PlaylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaylistController.class)
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaylistService playlistService;

    @Test
    void getPlaylistTracks_success_returnsPlaylistResponse() throws Exception {
        PlaylistResponse response = new PlaylistResponse("My Playlist", List.of(
                new TrackResponse("t1", "Song", "Artist", "Album", "img", "preview", "spotify")
        ));
        when(playlistService.getPlaylistWithTracks("my-token", "pl123")).thenReturn(response);

        mockMvc.perform(get("/playlist/pl123/tracks")
                        .header("Authorization", "Bearer my-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlistName").value("My Playlist"))
                .andExpect(jsonPath("$.tracks[0].id").value("t1"));
    }

    @Test
    void getPlaylistTracks_missingAuth_returns401() throws Exception {
        mockMvc.perform(get("/playlist/pl123/tracks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPlaylistTracks_spotifyApiException_returns500() throws Exception {
        when(playlistService.getPlaylistWithTracks("my-token", "pl123"))
                .thenThrow(new SpotifyApiException("Something went wrong"));

        mockMvc.perform(get("/playlist/pl123/tracks")
                        .header("Authorization", "Bearer my-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("spotify_api_error"))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void getPlaylistTracks_tooManyRequests_returns429() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "42");
        when(playlistService.getPlaylistWithTracks("my-token", "pl123"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                        headers, new byte[0], null));

        mockMvc.perform(get("/playlist/pl123/tracks")
                        .header("Authorization", "Bearer my-token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.error").value("rate_limited"));
    }
}
