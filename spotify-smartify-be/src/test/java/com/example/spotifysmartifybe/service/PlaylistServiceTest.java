package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.dto.PlaylistSummary;
import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PlaylistServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private PlaylistService playlistService;

    private static final String PLAYLIST_ID = "pl123";
    private static final String TOKEN = "test-token";
    private static final String CURRENT_USER_ID = "user123";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        playlistService = new PlaylistService(restTemplate);
    }

    // --- success ---

    @Test
    void getPlaylistWithTracks_success_returnsPlaylistResponse() {
        mockPlaylistMeta("My Playlist");
        mockPlaylistItemsPage("""
                {
                    "items": [{
                        "is_local": false,
                        "item": {
                            "id": "t1", "name": "Song", "type": "track",
                            "artists": [{"name": "Artist"}],
                            "album": {"name": "Album", "images": [{"url": "https://img.test/1.jpg"}]},
                            "preview_url": "https://preview.test/1.mp3",
                            "external_urls": {"spotify": "https://open.spotify.com/track/t1"}
                        }
                    }],
                    "next": null
                }
                """);

        PlaylistResponse result = playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID);

        assertThat(result.playlistName()).isEqualTo("My Playlist");
        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().id()).isEqualTo("t1");
        mockServer.verify();
    }

    @Test
    void getPlaylistWithTracks_mapsTrackFieldsCorrectly() {
        mockPlaylistMeta("Test Playlist");
        mockPlaylistItemsPage("""
                {
                    "items": [{
                        "is_local": false,
                        "item": {
                            "id": "track-42", "name": "Great Song", "type": "track",
                            "artists": [{"name": "Alice"}, {"name": "Bob"}],
                            "album": {"name": "Best Album", "images": [{"url": "https://img.test/cover.jpg"}]},
                            "preview_url": "https://preview.test/42.mp3",
                            "external_urls": {"spotify": "https://open.spotify.com/track/track-42"}
                        }
                    }],
                    "next": null
                }
                """);

        PlaylistResponse result = playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID);
        TrackResponse track = result.tracks().getFirst();

        assertThat(track.id()).isEqualTo("track-42");
        assertThat(track.name()).isEqualTo("Great Song");
        assertThat(track.artists()).isEqualTo("Alice, Bob");
        assertThat(track.albumName()).isEqualTo("Best Album");
        assertThat(track.albumImageUrl()).isEqualTo("https://img.test/cover.jpg");
        assertThat(track.previewUrl()).isEqualTo("https://preview.test/42.mp3");
        assertThat(track.spotifyUrl()).isEqualTo("https://open.spotify.com/track/track-42");
        mockServer.verify();
    }

    // --- filtering ---

    @Test
    void getPlaylistWithTracks_skipsLocalTracks() {
        mockPlaylistMeta("Playlist");
        mockPlaylistItemsPage("""
                {
                    "items": [
                        {"is_local": true, "item": {"id": "local1", "name": "Local Song", "type": "track"}},
                        {"is_local": false, "item": {"id": "t1", "name": "Real Song", "type": "track",
                            "artists": [{"name": "A"}], "album": {"name": "B"}}}
                    ],
                    "next": null
                }
                """);

        PlaylistResponse result = playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID);

        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().id()).isEqualTo("t1");
        mockServer.verify();
    }

    @Test
    void getPlaylistWithTracks_skipsNonTrackTypes() {
        mockPlaylistMeta("Playlist");
        mockPlaylistItemsPage("""
                {
                    "items": [
                        {"is_local": false, "item": {"id": "ep1", "name": "Episode", "type": "episode"}},
                        {"is_local": false, "item": {"id": "t1", "name": "Song", "type": "track",
                            "artists": [{"name": "A"}], "album": {"name": "B"}}}
                    ],
                    "next": null
                }
                """);

        PlaylistResponse result = playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID);

        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().id()).isEqualTo("t1");
        mockServer.verify();
    }

    // --- pagination ---

    @Test
    void getPlaylistWithTracks_paginatesUntilNextIsNull() {
        mockPlaylistMeta("Paginated");

        // Page 1 — has next
        mockServer.expect(requestTo(itemsUrl(0)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess("""
                        {
                            "items": [{"is_local": false, "item": {"id": "t1", "name": "S1", "type": "track",
                                "artists": [{"name": "A"}], "album": {"name": "B"}}}],
                            "next": "https://api.spotify.com/v1/playlists/pl123/items?offset=50"
                        }
                        """, MediaType.APPLICATION_JSON));

        // Page 2 — no next
        mockServer.expect(requestTo(itemsUrl(50)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "items": [{"is_local": false, "item": {"id": "t2", "name": "S2", "type": "track",
                                "artists": [{"name": "A"}], "album": {"name": "B"}}}],
                            "next": null
                        }
                        """, MediaType.APPLICATION_JSON));

        PlaylistResponse result = playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID);

        assertThat(result.tracks()).hasSize(2);
        assertThat(result.tracks().get(0).id()).isEqualTo("t1");
        assertThat(result.tracks().get(1).id()).isEqualTo("t2");
        mockServer.verify();
    }

    @Test
    void getPlaylistWithTracks_stopsAtMaxTracks() {
        mockPlaylistMeta("Huge Playlist");

        // Build 11 pages of 50 tracks each (550 total), but should stop at 500
        for (int page = 0; page < 10; page++) {
            StringBuilder items = new StringBuilder("[");
            for (int i = 0; i < 50; i++) {
                if (i > 0) items.append(",");
                String id = "t" + (page * 50 + i);
                items.append("""
                        {"is_local": false, "item": {"id": "%s", "name": "S", "type": "track",
                            "artists": [{"name": "A"}], "album": {"name": "B"}}}
                        """.formatted(id));
            }
            items.append("]");

            mockServer.expect(requestTo(itemsUrl(page * 50)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("""
                            {"items": %s, "next": "https://api.spotify.com/v1/next"}
                            """.formatted(items), MediaType.APPLICATION_JSON));
        }

        PlaylistResponse result = playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID);

        assertThat(result.tracks()).hasSize(500);
        mockServer.verify();
    }

    // --- error handling ---

    @Test
    void getPlaylistWithTracks_forbidden_throwsSpotifyApiException() {
        mockPlaylistMeta("Forbidden Playlist");
        mockServer.expect(requestTo(itemsUrl(0)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("not accessible");
        mockServer.verify();
    }

    @Test
    void getPlaylistWithTracks_notFound_throwsSpotifyApiException() {
        // 404 on playlist name fetch
        mockServer.expect(requestTo(metaUrl()))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("not found");
        mockServer.verify();
    }

    @Test
    void getPlaylistWithTracks_unauthorized_throwsSpotifyApiException() {
        mockServer.expect(requestTo(metaUrl()))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("expired or invalid");
        mockServer.verify();
    }

    @Test
    void getPlaylistWithTracks_tooManyRequests_propagates() {
        mockServer.expect(requestTo(metaUrl()))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "30"));

        assertThatThrownBy(() -> playlistService.getPlaylistWithTracks(TOKEN, PLAYLIST_ID))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class);
        mockServer.verify();
    }

    // --- helpers ---

    private String metaUrl() {
        return "https://api.spotify.com/v1/playlists/" + PLAYLIST_ID + "?fields=name";
    }

    private String itemsUrl(int offset) {
        return "https://api.spotify.com/v1/playlists/" + PLAYLIST_ID
                + "/items?limit=50&offset=" + offset + "&additional_types=track";
    }

    private void mockPlaylistMeta(String name) {
        mockServer.expect(requestTo(metaUrl()))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess("""
                        {"name": "%s"}
                        """.formatted(name), MediaType.APPLICATION_JSON));
    }

    private void mockPlaylistItemsPage(String responseJson) {
        mockServer.expect(requestTo(itemsUrl(0)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    // --- getUserPlaylists helpers ---

    private static final String ME_URL = "https://api.spotify.com/v1/me";

    private String userPlaylistsUrl(int offset) {
        return "https://api.spotify.com/v1/me/playlists?limit=50&offset=" + offset;
    }

    private void mockCurrentUser() {
        mockServer.expect(requestTo(ME_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess("""
                        {"id": "%s"}
                        """.formatted(CURRENT_USER_ID), MediaType.APPLICATION_JSON));
    }

    private String playlistJson(String id, String name, String ownerId, boolean collaborative) {
        return """
                {
                    "id": "%s", "name": "%s", "collaborative": %s,
                    "owner": {"id": "%s", "display_name": "Owner"},
                    "images": [{"url": "https://img.test/%s.jpg"}],
                    "tracks": {"total": 10}
                }
                """.formatted(id, name, collaborative, ownerId, id);
    }

    // --- getUserPlaylists tests ---

    @Test
    void getUserPlaylists_returnsOwnedAndCollaborativePlaylists() {
        mockCurrentUser();
        mockServer.expect(requestTo(userPlaylistsUrl(0)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess("""
                        {
                            "items": [
                                %s,
                                %s,
                                %s
                            ],
                            "next": null
                        }
                        """.formatted(
                        playlistJson("p1", "My Playlist", CURRENT_USER_ID, false),
                        playlistJson("p2", "Collab Mix", "other-user", true),
                        playlistJson("p3", "Followed Public", "other-user", false)
                ), MediaType.APPLICATION_JSON));

        List<PlaylistSummary> result = playlistService.getUserPlaylists(TOKEN);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("p1");
        assertThat(result.get(0).name()).isEqualTo("My Playlist");
        assertThat(result.get(1).id()).isEqualTo("p2");
        assertThat(result.get(1).collaborative()).isTrue();
        mockServer.verify();
    }

    @Test
    void getUserPlaylists_filtersOutAllFollowedPlaylists() {
        mockCurrentUser();
        mockServer.expect(requestTo(userPlaylistsUrl(0)))
                .andRespond(withSuccess("""
                        {
                            "items": [
                                %s,
                                %s
                            ],
                            "next": null
                        }
                        """.formatted(
                        playlistJson("p1", "Public A", "stranger1", false),
                        playlistJson("p2", "Public B", "stranger2", false)
                ), MediaType.APPLICATION_JSON));

        List<PlaylistSummary> result = playlistService.getUserPlaylists(TOKEN);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void getUserPlaylists_mapsFieldsCorrectly() {
        mockCurrentUser();
        mockServer.expect(requestTo(userPlaylistsUrl(0)))
                .andRespond(withSuccess("""
                        {
                            "items": [
                                %s
                            ],
                            "next": null
                        }
                        """.formatted(playlistJson("p1", "My Playlist", CURRENT_USER_ID, false)),
                        MediaType.APPLICATION_JSON));

        List<PlaylistSummary> result = playlistService.getUserPlaylists(TOKEN);

        assertThat(result).hasSize(1);
        PlaylistSummary pl = result.getFirst();
        assertThat(pl.id()).isEqualTo("p1");
        assertThat(pl.name()).isEqualTo("My Playlist");
        assertThat(pl.imageUrl()).isEqualTo("https://img.test/p1.jpg");
        assertThat(pl.trackCount()).isEqualTo(10);
        assertThat(pl.ownerName()).isEqualTo("Owner");
        assertThat(pl.collaborative()).isFalse();
        mockServer.verify();
    }

    @Test
    void getUserPlaylists_emptyResponse_returnsEmptyList() {
        mockCurrentUser();
        mockServer.expect(requestTo(userPlaylistsUrl(0)))
                .andRespond(withSuccess("""
                        {"items": [], "next": null}
                        """, MediaType.APPLICATION_JSON));

        List<PlaylistSummary> result = playlistService.getUserPlaylists(TOKEN);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void getUserPlaylists_paginatesAndFilters() {
        mockCurrentUser();
        // Page 1
        mockServer.expect(requestTo(userPlaylistsUrl(0)))
                .andRespond(withSuccess("""
                        {
                            "items": [
                                %s,
                                %s
                            ],
                            "next": "https://api.spotify.com/v1/me/playlists?offset=50"
                        }
                        """.formatted(
                        playlistJson("p1", "Mine", CURRENT_USER_ID, false),
                        playlistJson("p2", "Followed", "other", false)
                ), MediaType.APPLICATION_JSON));
        // Page 2
        mockServer.expect(requestTo(userPlaylistsUrl(50)))
                .andRespond(withSuccess("""
                        {
                            "items": [
                                %s
                            ],
                            "next": null
                        }
                        """.formatted(playlistJson("p3", "Collab", "other", true)),
                        MediaType.APPLICATION_JSON));

        List<PlaylistSummary> result = playlistService.getUserPlaylists(TOKEN);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("p1");
        assertThat(result.get(1).id()).isEqualTo("p3");
        mockServer.verify();
    }

    @Test
    void getUserPlaylists_unauthorized_throwsSpotifyApiException() {
        mockServer.expect(requestTo(ME_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> playlistService.getUserPlaylists(TOKEN))
                .isInstanceOf(SpotifyApiException.class)
                .hasMessageContaining("expired or invalid");
        mockServer.verify();
    }
}
