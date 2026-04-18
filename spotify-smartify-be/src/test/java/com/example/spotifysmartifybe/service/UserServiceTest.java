package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SpotifyApiFactory spotifyApiFactory;

    @InjectMocks
    private UserService userService;

    private SpotifyApi mockSpotifyApi;

    @BeforeEach
    void setUp() {
        mockSpotifyApi = mock(SpotifyApi.class, RETURNS_DEEP_STUBS);
    }

    // --- getCurrentUserProfile ---

    @Test
    void getCurrentUserProfile_success_returnsUser() throws Exception {
        User user = new User.Builder().setId("user-1").setDisplayName("Test User").build();
        GetCurrentUsersProfileRequest mockRequest = mock(GetCurrentUsersProfileRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getCurrentUsersProfile().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(user);

        User result = userService.getCurrentUserProfile("token");

        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void getCurrentUserProfile_unauthorized_rethrows() throws Exception {
        GetCurrentUsersProfileRequest mockRequest = mock(GetCurrentUsersProfileRequest.class);

        when(spotifyApiFactory.createWithAccessToken("bad-token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getCurrentUsersProfile().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new UnauthorizedException("Unauthorized"));

        assertThatThrownBy(() -> userService.getCurrentUserProfile("bad-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUserProfile_tooManyRequests_rethrows() throws Exception {
        GetCurrentUsersProfileRequest mockRequest = mock(GetCurrentUsersProfileRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getCurrentUsersProfile().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new TooManyRequestsException("Rate limited", 30));

        assertThatThrownBy(() -> userService.getCurrentUserProfile("token"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void getCurrentUserProfile_otherException_wraps() throws Exception {
        GetCurrentUsersProfileRequest mockRequest = mock(GetCurrentUsersProfileRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getCurrentUsersProfile().build()).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new RuntimeException("Unexpected"));

        assertThatThrownBy(() -> userService.getCurrentUserProfile("token"))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("Failed to fetch user profile");
    }

    // --- getTopTracks ---

    @Test
    void getTopTracks_success_returnsTrackResponses() throws Exception {
        Track track = buildTrack("t1", "Song One");
        Paging<Track> paging = buildPaging(new Track[]{track});
        GetUsersTopTracksRequest mockRequest = mock(GetUsersTopTracksRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getUsersTopTracks().limit(50).time_range("medium_term").build())
                .thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(paging);

        List<TrackResponse> result = userService.getTopTracks("token", "medium_term");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("t1");
        assertThat(result.getFirst().name()).isEqualTo("Song One");
    }

    @Test
    void getTopTracks_nullItems_returnsEmptyList() throws Exception {
        Paging<Track> paging = buildPaging(null);
        GetUsersTopTracksRequest mockRequest = mock(GetUsersTopTracksRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getUsersTopTracks().limit(50).time_range("medium_term").build())
                .thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(paging);

        List<TrackResponse> result = userService.getTopTracks("token", "medium_term");

        assertThat(result).isEmpty();
    }

    @Test
    void getTopTracks_mapsFieldsCorrectly() throws Exception {
        ArtistSimplified artist1 = new ArtistSimplified.Builder().setName("Artist A").build();
        ArtistSimplified artist2 = new ArtistSimplified.Builder().setName("Artist B").build();
        Image image = new Image.Builder().setUrl("https://img.spotify.com/album.jpg").build();
        AlbumSimplified album = new AlbumSimplified.Builder()
                .setName("Great Album")
                .setImages(image)
                .build();
        ExternalUrl externalUrl = new ExternalUrl.Builder().setExternalUrls(
                java.util.Map.of("spotify", "https://open.spotify.com/track/t1")
        ).build();
        Track track = new Track.Builder()
                .setId("t1")
                .setName("Full Track")
                .setArtists(artist1, artist2)
                .setAlbum(album)
                .setPreviewUrl("https://preview.spotify.com/track.mp3")
                .setExternalUrls(externalUrl)
                .build();
        Paging<Track> paging = buildPaging(new Track[]{track});
        GetUsersTopTracksRequest mockRequest = mock(GetUsersTopTracksRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getUsersTopTracks().limit(50).time_range("short_term").build())
                .thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(paging);

        List<TrackResponse> result = userService.getTopTracks("token", "short_term");

        assertThat(result).hasSize(1);
        TrackResponse tr = result.getFirst();
        assertThat(tr.id()).isEqualTo("t1");
        assertThat(tr.name()).isEqualTo("Full Track");
        assertThat(tr.artists()).isEqualTo("Artist A, Artist B");
        assertThat(tr.albumName()).isEqualTo("Great Album");
        assertThat(tr.albumImageUrl()).isEqualTo("https://img.spotify.com/album.jpg");
        assertThat(tr.previewUrl()).isEqualTo("https://preview.spotify.com/track.mp3");
        assertThat(tr.spotifyUrl()).isEqualTo("https://open.spotify.com/track/t1");
    }

    @Test
    void getTopTracks_unauthorized_rethrows() throws Exception {
        GetUsersTopTracksRequest mockRequest = mock(GetUsersTopTracksRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getUsersTopTracks().limit(50).time_range("medium_term").build())
                .thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new UnauthorizedException("Unauthorized"));

        assertThatThrownBy(() -> userService.getTopTracks("token", "medium_term"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getTopTracks_tooManyRequests_rethrows() throws Exception {
        GetUsersTopTracksRequest mockRequest = mock(GetUsersTopTracksRequest.class);

        when(spotifyApiFactory.createWithAccessToken("token")).thenReturn(mockSpotifyApi);
        when(mockSpotifyApi.getUsersTopTracks().limit(50).time_range("medium_term").build())
                .thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new TooManyRequestsException("Rate limited", 30));

        assertThatThrownBy(() -> userService.getTopTracks("token", "medium_term"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    // --- helpers ---

    private Track buildTrack(String id, String name) {
        ArtistSimplified artist = new ArtistSimplified.Builder().setName("Test Artist").build();
        AlbumSimplified album = new AlbumSimplified.Builder().setName("Test Album").build();
        return new Track.Builder()
                .setId(id)
                .setName(name)
                .setArtists(artist)
                .setAlbum(album)
                .build();
    }

    private Paging<Track> buildPaging(Track[] items) {
        return new Paging.Builder<Track>().setItems(items).build();
    }
}
