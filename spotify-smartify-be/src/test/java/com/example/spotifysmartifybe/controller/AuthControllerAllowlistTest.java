package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.service.AuthService;
import com.example.spotifysmartifybe.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.allowed-spotify-ids=allowed-user-1,allowed-user-2")
class AuthControllerAllowlistTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @Test
    void callback_userNotAllowed_redirectsWithAccessDenied() throws Exception {
        AuthorizationCodeCredentials credentials = new AuthorizationCodeCredentials.Builder()
                .setAccessToken("access-123")
                .setRefreshToken("refresh-456")
                .setExpiresIn(3600)
                .build();
        when(authService.exchangeCode("test-code")).thenReturn(credentials);

        User user = new User.Builder().setId("not-allowed-user").build();
        when(userService.getCurrentUserProfile("access-123")).thenReturn(user);

        mockMvc.perform(get("/auth/callback").param("code", "test-code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/?error=access_denied"));
    }
}
