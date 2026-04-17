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
            log.info("403 for playlist {} — Dev Mode restricts access to owned/collaborative playlists only", playlistId);
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
            if (body == null || body.name == null) {
                throw new SpotifyApiException("Playlist not found");
            }
            return body.name;
        } catch (HttpClientErrorException.NotFound e) {
            throw new SpotifyApiException("Playlist not found: " + playlistId);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new SpotifyApiException("Access token expired or invalid");
        } catch (HttpClientErrorException e) {
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
            if (page == null || page.items == null) {
                break;
            }

            for (SpotifyPlaylistItem playlistItem : page.items) {
                if (playlistItem.isLocal) continue;
                SpotifyTrack track = playlistItem.item;
                if (track == null || !"track".equals(track.type)) continue;

                allTracks.add(toTrackResponse(track));
            }

            if (page.next == null) break;
            offset += PAGE_LIMIT;
        }

        return allTracks;
    }

    private TrackResponse toTrackResponse(SpotifyTrack track) {
        String artistNames = (track.artists != null)
                ? track.artists.stream().map(a -> a.name).collect(Collectors.joining(", "))
                : "";

        String albumName = (track.album != null) ? track.album.name : "";

        String albumImageUrl = "";
        if (track.album != null && track.album.images != null && !track.album.images.isEmpty()) {
            albumImageUrl = track.album.images.get(0).url;
        }

        String previewUrl = (track.previewUrl != null) ? track.previewUrl : "";

        String spotifyUrl = "";
        if (track.externalUrls != null) {
            spotifyUrl = track.externalUrls.getOrDefault("spotify", "");
        }

        return new TrackResponse(
                track.id, track.name, artistNames, albumName,
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
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyPagingResponse {
        public List<SpotifyPlaylistItem> items;
        public String next;
        public int total;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyPlaylistItem {
        @JsonProperty("is_local")
        public boolean isLocal;

        // Feb 2026: field renamed from "track" to "item"
        @JsonProperty("item")
        public SpotifyTrack item;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyTrack {
        public String id;
        public String name;
        public String type;
        public List<SpotifyArtist> artists;
        public SpotifyAlbum album;
        @JsonProperty("preview_url")
        public String previewUrl;
        @JsonProperty("external_urls")
        public Map<String, String> externalUrls;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyArtist {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyAlbum {
        public String name;
        public List<SpotifyImage> images;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SpotifyImage {
        public String url;
    }
}
