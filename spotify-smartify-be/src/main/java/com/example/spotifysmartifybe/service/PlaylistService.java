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
import org.springframework.web.util.UriComponentsBuilder;

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
        String url = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE)
                .pathSegment("playlists", playlistId)
                .queryParam("fields", "name")
                .toUriString();

        try {
            ResponseEntity<SpotifyPlaylistMeta> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(accessToken), SpotifyPlaylistMeta.class);

            SpotifyPlaylistMeta body = response.getBody();
            if (body == null || body.name() == null) {
                throw new SpotifyApiException("Playlist not found");
            }
            return body.name();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Playlist not found: {}", playlistId, e);
            throw new SpotifyApiException("Playlist not found: " + playlistId);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Access token expired or invalid for playlist: {}", playlistId, e);
            throw new SpotifyApiException("Access token expired or invalid");
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e; // handled by GlobalExceptionHandler
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch playlist info for {}: {}", playlistId, e.getStatusCode(), e);
            throw new SpotifyApiException("Failed to fetch playlist info: " + e.getStatusCode());
        }
    }

    private List<TrackResponse> fetchPlaylistItems(String token, String playlistId) {
        List<TrackResponse> allTracks = new ArrayList<>();
        int offset = 0;

        while (allTracks.size() < MAX_TRACKS) {
            SpotifyPagingResponse page = fetchPlaylistItemsPage(token, playlistId, offset);
            if (page == null || page.items() == null) {
                break;
            }

            for (SpotifyPlaylistItem playlistItem : page.items()) {
                if (playlistItem.isLocal()) {
                    continue;
                }
                SpotifyTrack track = playlistItem.track();
                if (track == null || !"track".equals(track.type())) {
                    continue;
                }

                allTracks.add(toTrackResponse(track));
            }

            if (page.next() == null) {
                break;
            }
            offset += PAGE_LIMIT;
        }

        log.info("Fetched {} tracks from playlist {}", allTracks.size(), playlistId);
        return allTracks;
    }

    private SpotifyPagingResponse fetchPlaylistItemsPage(String token, String playlistId, int offset) {
        String url = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE)
                .pathSegment("playlists", playlistId, "items")
                .queryParam("limit", PAGE_LIMIT)
                .queryParam("offset", offset)
                .queryParam("additional_types", "track")
                .toUriString();

        try {
            ResponseEntity<SpotifyPagingResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(token), SpotifyPagingResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Playlist not found: {}", playlistId, e);
            throw new SpotifyApiException("Playlist not found: " + playlistId);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Access token expired or invalid for playlist: {}", playlistId, e);
            throw new SpotifyApiException("Access token expired or invalid");
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e; // handled by GlobalExceptionHandler
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch playlist items for {}: {}", playlistId, e.getStatusCode(), e);
            throw new SpotifyApiException("Failed to fetch playlist items: " + e.getStatusCode());
        }
    }

    private TrackResponse toTrackResponse(SpotifyTrack track) {
        String artistNames = (track.artists() != null)
                ? track.artists().stream().map(SpotifyArtist::name).collect(Collectors.joining(", "))
                : "";

        String albumName = (track.album() != null) ? track.album().name() : "";

        String albumImageUrl = "";
        if (track.album() != null && track.album().images() != null && !track.album().images().isEmpty()) {
            albumImageUrl = track.album().images().getFirst().url();
        }

        String previewUrl = (track.previewUrl() != null) ? track.previewUrl() : "";

        String spotifyUrl = "";
        if (track.externalUrls() != null) {
            spotifyUrl = track.externalUrls().getOrDefault("spotify", "");
        }

        return new TrackResponse(
                track.id(), track.name(), artistNames, albumName,
                albumImageUrl, previewUrl, spotifyUrl);
    }

    private HttpEntity<Void> authEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    // --- Spotify API response DTOs ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistMeta(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPagingResponse(List<SpotifyPlaylistItem> items, String next) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistItem(@JsonProperty("is_local") boolean isLocal, @JsonProperty("item") SpotifyTrack track) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyTrack(
            String id,
            String name,
            String type,
            List<SpotifyArtist> artists,
            SpotifyAlbum album,
            @JsonProperty("preview_url") String previewUrl,
            @JsonProperty("external_urls") Map<String, String> externalUrls) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyArtist(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyAlbum(String name, List<SpotifyImage> images) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyImage(String url) {}
}
