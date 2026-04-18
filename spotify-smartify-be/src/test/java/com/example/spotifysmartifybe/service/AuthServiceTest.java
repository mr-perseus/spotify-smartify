package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SpotifyApiFactory spotifyApiFactory;

    @InjectMocks
    private AuthService authService;

    private SpotifyApi mockSpotifyApi;

    @BeforeEach
    void setUp() {
        mockSpotifyApi = mock(SpotifyApi.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void getAuthorizationUri_returnsUri() {
        URI expectedUri = URI.create("https://accounts.spotify.com/authorize?scope=test");
        AuthorizationCodeUriRequest mockUriRequest = mock(AuthorizationCodeUriRequest.class);

        when(spotifyApiFactory.createForAuth()).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCodeUri().scope(anyString()).show_dialog(anyBoolean()).build())
                .thenReturn(mockUriRequest);
        when(mockUriRequest.execute()).thenReturn(expectedUri);

        URI result = authService.getAuthorizationUri();

        assertThat(result).isEqualTo(expectedUri);
    }

    @Test
    void exchangeCode_success_returnsCredentials() throws Exception {
        AuthorizationCodeCredentials credentials = new AuthorizationCodeCredentials.Builder()
                .setAccessToken("access-token")
                .setRefreshToken("refresh-token")
                .setExpiresIn(3600)
                .build();
        AuthorizationCodeRequest mockRequest = mock(AuthorizationCodeRequest.class);

        when(spotifyApiFactory.createForAuth()).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCode(anyString()).build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(credentials);

        AuthorizationCodeCredentials result = authService.exchangeCode("test-code");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
    }

    @Test
    void exchangeCode_tooManyRequests_rethrows() throws Exception {
        AuthorizationCodeRequest mockRequest = mock(AuthorizationCodeRequest.class);

        when(spotifyApiFactory.createForAuth()).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCode(anyString()).build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new TooManyRequestsException("Rate limited", 30));

        assertThatThrownBy(() -> authService.exchangeCode("test-code"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void exchangeCode_otherException_throwsSpotifyApiException() throws Exception {
        AuthorizationCodeRequest mockRequest = mock(AuthorizationCodeRequest.class);

        when(spotifyApiFactory.createForAuth()).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCode(anyString()).build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new RuntimeException("Something broke"));

        assertThatThrownBy(() -> authService.exchangeCode("test-code"))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("Failed to exchange Spotify authorization code");
    }

    @Test
    void refreshAccessToken_success_returnsCredentials() throws Exception {
        AuthorizationCodeCredentials credentials = new AuthorizationCodeCredentials.Builder()
                .setAccessToken("new-access-token")
                .setExpiresIn(3600)
                .build();
        AuthorizationCodeRefreshRequest mockRequest = mock(AuthorizationCodeRefreshRequest.class);

        when(spotifyApiFactory.createWithRefreshToken("refresh-token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCodeRefresh().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(credentials);

        AuthorizationCodeCredentials result = authService.refreshAccessToken("refresh-token");

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
    }

    @Test
    void refreshAccessToken_tooManyRequests_rethrows() throws Exception {
        AuthorizationCodeRefreshRequest mockRequest = mock(AuthorizationCodeRefreshRequest.class);

        when(spotifyApiFactory.createWithRefreshToken("refresh-token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCodeRefresh().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new TooManyRequestsException("Rate limited", 30));

        assertThatThrownBy(() -> authService.refreshAccessToken("refresh-token"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void refreshAccessToken_otherException_throwsSpotifyApiException() throws Exception {
        AuthorizationCodeRefreshRequest mockRequest = mock(AuthorizationCodeRefreshRequest.class);

        when(spotifyApiFactory.createWithRefreshToken("refresh-token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.authorizationCodeRefresh().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new RuntimeException("Something broke"));

        assertThatThrownBy(() -> authService.refreshAccessToken("refresh-token"))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("Failed to refresh access token");
    }
}
