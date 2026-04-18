package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.service.AuthService;
import com.example.spotifysmartifybe.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @Test
    void login_returnsAuthorizationUrl() throws Exception {
        when(authService.getAuthorizationUri())
                .thenReturn(URI.create("https://accounts.spotify.com/authorize?scope=test"));

        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl")
                        .value("https://accounts.spotify.com/authorize?scope=test"));
    }

    @Test
    void callback_success_redirectsWithTokens() throws Exception {
        AuthorizationCodeCredentials creds = new AuthorizationCodeCredentials.Builder()
                .setAccessToken("access-123")
                .setRefreshToken("refresh-456")
                .setExpiresIn(3600)
                .build();
        when(authService.exchangeCode("test-code")).thenReturn(creds);

        mockMvc.perform(get("/auth/callback").param("code", "test-code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/callback?accessToken=access-123&refreshToken=refresh-456&expiresIn=3600"));
    }

    @Test
    void callback_withError_redirectsWithError() throws Exception {
        mockMvc.perform(get("/auth/callback").param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/?error=access_denied"));
    }

    @Test
    void callback_missingCode_redirectsWithMissingCodeError() throws Exception {
        mockMvc.perform(get("/auth/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/?error=missing_code"));
    }

    @Test
    void callback_exchangeFails_redirectsWithExchangeFailedError() throws Exception {
        when(authService.exchangeCode("bad-code")).thenThrow(new RuntimeException("Exchange failed"));

        mockMvc.perform(get("/auth/callback").param("code", "bad-code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/?error=exchange_failed"));
    }

    @Test
    void refresh_success_returnsNewTokens() throws Exception {
        AuthorizationCodeCredentials creds = new AuthorizationCodeCredentials.Builder()
                .setAccessToken("new-access")
                .setExpiresIn(3600)
                .build();
        when(authService.refreshAccessToken("refresh-token")).thenReturn(creds);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void refresh_missingRefreshToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

}
