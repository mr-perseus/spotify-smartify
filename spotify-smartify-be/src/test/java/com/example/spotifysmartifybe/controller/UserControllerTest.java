package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // --- getProfile ---

    @Test
    void getProfile_success_returnsUserProfile() throws Exception {
        User user = new User.Builder()
                .setDisplayName("Test User")
                .setEmail("test@example.com")
                .build();
        when(userService.getCurrentUserProfile("my-token")).thenReturn(user);

        mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer my-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getProfile_missingAuth_returns401() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_unauthorized_returns401() throws Exception {
        when(userService.getCurrentUserProfile("bad-token"))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void getProfile_tooManyRequests_returns429() throws Exception {
        when(userService.getCurrentUserProfile("my-token"))
                .thenThrow(new TooManyRequestsException("Rate limited", 30));

        mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer my-token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.error").value("rate_limited"))
                .andExpect(jsonPath("$.retryAfter").value("30"));
    }

    // --- getTopTracks ---

    @Test
    void getTopTracks_success_returnsTracks() throws Exception {
        List<TrackResponse> tracks = List.of(
                new TrackResponse("t1", "Song", "Artist", "Album", "img", "preview", "spotify")
        );
        when(userService.getTopTracks("my-token", "medium_term")).thenReturn(tracks);

        mockMvc.perform(get("/user/top-tracks")
                        .header("Authorization", "Bearer my-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"))
                .andExpect(jsonPath("$[0].name").value("Song"));
    }

    @Test
    void getTopTracks_invalidTimeRange_returns400() throws Exception {
        mockMvc.perform(get("/user/top-tracks")
                        .header("Authorization", "Bearer my-token")
                        .param("timeRange", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTopTracks_missingAuth_returns401() throws Exception {
        mockMvc.perform(get("/user/top-tracks"))
                .andExpect(status().isUnauthorized());
    }
}
