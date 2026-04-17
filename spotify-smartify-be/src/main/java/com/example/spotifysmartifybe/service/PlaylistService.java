package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";
    private static final int PAGE_LIMIT = 50;
    private static final int MAX_TRACKS = 500;

    private final RestTemplate restTemplate;

    public PlaylistResponse getPlaylistWithTracks(String accessToken, String playlistId) {
        String playlistName = fetchPlaylistName(accessToken, playlistId);

        try {
            List<TrackResponse> tracks = fetchPlaylistItems(accessToken, playlistId);
            return new PlaylistResponse(playlistName, tracks);
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden for playlist {} — Dev Mode restricts access to owned/collaborative playlists only", playlistId, e);
            throw new SpotifyApiException(
                    "This playlist is not accessible. Spotify's Dev Mode only allows access " +
                    "to playlists you own or collaborate on.");
        }
    }

    private String fetchPlaylistName(String accessToken, String playlistId) {
        String url = SPOTIFY_API_BASE + "/playlists/" + playlistId + "?fields=name";

        try {
            ResponseEntity<SpotifyPlaylistMeta> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(accessToken), SpotifyPlaylistMeta.class);

            SpotifyPlaylistMeta body = response.getBody();
            if (body == null || body.getName() == null) {
                throw new SpotifyApiException("Playlist not found");
            }
            return body.getName();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Playlist not found: {}", playlistId, e);
            throw new SpotifyApiException("Playlist not found: " + playlistId);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Access token expired or invalid for playlist: {}", playlistId, e);
            throw new SpotifyApiException("Access token expired or invalid");
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch playlist info for {}: {}", playlistId, e.getStatusCode(), e);
            throw new SpotifyApiException("Failed to fetch playlist info: " + e.getStatusCode());
        }
    }

    private List<TrackResponse> fetchPlaylistItems(String token, String playlistId) {
        List<TrackResponse> allTracks = new ArrayList<>();
        int offset = 0;

        while (allTracks.size() < MAX_TRACKS) {
            String url = SPOTIFY_API_BASE + "/playlists/" + playlistId
                    + "/items?limit=" + PAGE_LIMIT
                    + "&offset=" + offset
                    + "&additional_types=track";

            ResponseEntity<SpotifyPagingResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(token), SpotifyPagingResponse.class);

            SpotifyPagingResponse page = response.getBody();
            if (page == null || page.getItems() == null) {
                break;
            }

            for (SpotifyPlaylistItem playlistItem : page.getItems()) {
                if (playlistItem.isLocal()) {
                    continue;
                }
                SpotifyTrack track = playlistItem.getItem();
                if (track == null || !"track".equals(track.getType())) {
                    continue;
                }

                allTracks.add(toTrackResponse(track));
            }

            if (page.getNext() == null) {
                break;
            }
            offset += PAGE_LIMIT;
        }

        return allTracks;
    }

    private TrackResponse toTrackResponse(SpotifyTrack track) {
        String artistNames = (track.getArtists() != null)
                ? track.getArtists().stream().map(SpotifyArtist::getName).collect(Collectors.joining(", "))
                : "";

        String albumName = (track.getAlbum() != null) ? track.getAlbum().getName() : "";

        String albumImageUrl = "";
        if (track.getAlbum() != null && track.getAlbum().getImages() != null && !track.getAlbum().getImages().isEmpty()) {
            albumImageUrl = track.getAlbum().getImages().get(0).getUrl();
        }

        String previewUrl = (track.getPreviewUrl() != null) ? track.getPreviewUrl() : "";

        String spotifyUrl = "";
        if (track.getExternalUrls() != null) {
            spotifyUrl = track.getExternalUrls().getOrDefault("spotify", "");
        }

        return new TrackResponse(
                track.getId(), track.getName(), artistNames, albumName,
                albumImageUrl, previewUrl, spotifyUrl);
    }

    private HttpEntity<Void> authEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    // --- Spotify API response DTOs (Jackson) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyPlaylistMeta {
        @JsonProperty
        private String name;

        public String getName() {
            return name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyPagingResponse {
        @JsonProperty
        private List<SpotifyPlaylistItem> items;
        @JsonProperty
        private String next;
        @JsonProperty
        private int total;

        public List<SpotifyPlaylistItem> getItems() {
            return items;
        }

        public String getNext() {
            return next;
        }

        public int getTotal() {
            return total;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyPlaylistItem {
        @JsonProperty("is_local")
        private boolean isLocal;

        @JsonProperty("item")
        private SpotifyTrack item;

        public boolean isLocal() {
            return isLocal;
        }

        public SpotifyTrack getItem() {
            return item;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyTrack {
        @JsonProperty
        private String id;
        @JsonProperty
        private String name;
        @JsonProperty
        private String type;
        @JsonProperty
        private List<SpotifyArtist> artists;
        @JsonProperty
        private SpotifyAlbum album;
        @JsonProperty("preview_url")
        private String previewUrl;
        @JsonProperty("external_urls")
        private Map<String, String> externalUrls;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public List<SpotifyArtist> getArtists() {
            return artists;
        }

        public SpotifyAlbum getAlbum() {
            return album;
        }

        public String getPreviewUrl() {
            return previewUrl;
        }

        public Map<String, String> getExternalUrls() {
            return externalUrls;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyArtist {
        @JsonProperty
        private String name;

        public String getName() {
            return name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyAlbum {
        @JsonProperty
        private String name;
        @JsonProperty
        private List<SpotifyImage> images;

        public String getName() {
            return name;
        }

        public List<SpotifyImage> getImages() {
            return images;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyImage {
        @JsonProperty
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
