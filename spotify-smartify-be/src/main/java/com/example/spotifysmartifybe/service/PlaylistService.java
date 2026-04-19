package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.dto.PlaylistSummary;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";
    private static final int PAGE_LIMIT = 50;
    private static final int MAX_TRACKS = 500;
    private static final int MAX_PLAYLISTS = 1000;

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

    public List<PlaylistSummary> getUserPlaylists(String accessToken) {
        String currentUserId = fetchCurrentUserId(accessToken);

        List<PlaylistSummary> playlists = fetchAllPages(
                accessToken,
                MAX_PLAYLISTS,
                offset -> UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE)
                        .pathSegment("me", "playlists")
                        .queryParam("limit", PAGE_LIMIT)
                        .queryParam("offset", offset)
                        .toUriString(),
                SpotifyPlaylistListResponse.class,
                "user playlists",
                SpotifyPlaylistListResponse::items,
                SpotifyPlaylistListResponse::next,
                sp -> toPlaylistSummaryOrNull(sp, currentUserId)
        );
        log.info("Fetched {} owned/collaborative playlists for user {}", playlists.size(), currentUserId);
        return playlists;
    }

    private String fetchCurrentUserId(String accessToken) {
        String url = SPOTIFY_API_BASE + "/me";
        SpotifyCurrentUser user = spotifyGet(url, accessToken, SpotifyCurrentUser.class, "current user");
        if (user == null || user.id() == null) {
            throw new SpotifyApiException("Could not determine current user");
        }
        return user.id();
    }

    /**
     * Maps a Spotify playlist to a PlaylistSummary, returning null for playlists
     * the user neither owns nor collaborates on (i.e. followed public playlists).
     */
    private PlaylistSummary toPlaylistSummaryOrNull(SpotifySimplifiedPlaylist sp, String currentUserId) {
        boolean isOwner = sp.owner() != null && currentUserId.equals(sp.owner().id());
        if (!isOwner && !sp.collaborative()) {
            return null;
        }
        return toPlaylistSummary(sp);
    }

    private PlaylistSummary toPlaylistSummary(SpotifySimplifiedPlaylist sp) {
        String imageUrl = (sp.images() != null && !sp.images().isEmpty())
                ? sp.images().getFirst().url() : null;
        int trackCount = (sp.items() != null) ? sp.items().total() : 0;
        String ownerName = (sp.owner() != null && sp.owner().displayName() != null)
                ? sp.owner().displayName() : "";
        return new PlaylistSummary(sp.id(), sp.name(), imageUrl, trackCount, ownerName, sp.collaborative());
    }

    private String fetchPlaylistName(String accessToken, String playlistId) {
        String url = UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE)
                .pathSegment("playlists", playlistId)
                .queryParam("fields", "name")
                .toUriString();

        SpotifyPlaylistMeta body = spotifyGet(url, accessToken, SpotifyPlaylistMeta.class, "playlist " + playlistId);
        if (body == null || body.name() == null) {
            throw new SpotifyApiException("Playlist not found");
        }
        return body.name();
    }

    private List<TrackResponse> fetchPlaylistItems(String token, String playlistId) {
        List<TrackResponse> tracks = fetchAllPages(
                token,
                MAX_TRACKS,
                offset -> UriComponentsBuilder.fromUriString(SPOTIFY_API_BASE)
                        .pathSegment("playlists", playlistId, "items")
                        .queryParam("limit", PAGE_LIMIT)
                        .queryParam("offset", offset)
                        .queryParam("additional_types", "track")
                        .toUriString(),
                SpotifyPagingResponse.class,
                "playlist items for " + playlistId,
                SpotifyPagingResponse::items,
                SpotifyPagingResponse::next,
                this::toTrackOrNull
        );
        log.info("Fetched {} tracks from playlist {}", tracks.size(), playlistId);
        return tracks;
    }

    private TrackResponse toTrackOrNull(SpotifyPlaylistItem playlistItem) {
        if (playlistItem.isLocal()) return null;
        SpotifyTrack track = playlistItem.track();
        if (track == null || !"track".equals(track.type())) return null;
        return toTrackResponse(track);
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

    private <T> T spotifyGet(String url, String token, Class<T> responseType, String context) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, authEntity(token), responseType);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("{} not found", context, e);
            throw new SpotifyApiException(context + " not found");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Access token expired or invalid for {}", context, e);
            throw new SpotifyApiException("Access token expired or invalid");
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e; // handled by GlobalExceptionHandler
        } catch (HttpClientErrorException.Forbidden e) {
            throw e; // handled by caller (e.g. getPlaylistWithTracks)
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch {}: {}", context, e.getStatusCode(), e);
            throw new SpotifyApiException("Failed to fetch " + context + ": " + e.getStatusCode());
        }
    }

    private HttpEntity<Void> authEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    /**
     * Generic paginator: fetches all pages from a Spotify paging endpoint,
     * mapping each item through {@code mapper}. Items for which mapper returns
     * {@code null} are silently skipped (useful for filtering).
     */
    private <P, I, R> List<R> fetchAllPages(
            String token,
            int maxResults,
            Function<Integer, String> urlBuilder,
            Class<P> pageType,
            String context,
            Function<P, List<I>> itemsExtractor,
            Function<P, String> nextExtractor,
            Function<I, R> mapper
    ) {
        List<R> all = new ArrayList<>();
        int offset = 0;
        while (all.size() < maxResults) {
            P page = spotifyGet(urlBuilder.apply(offset), token, pageType, context);
            List<I> items = (page != null) ? itemsExtractor.apply(page) : null;
            if (items == null) {
                break;
            }

            addMappedItems(all, items, mapper, maxResults);

            if (!shouldContinuePagination(all.size(), maxResults, nextExtractor.apply(page))) {
                break;
            }

            offset += PAGE_LIMIT;
        }
        return all;
    }

    private <I, R> void addMappedItems(List<R> results, List<I> items, Function<I, R> mapper, int maxResults) {
        for (I item : items) {
            if (results.size() >= maxResults) {
                return;
            }

            R mapped = mapper.apply(item);
            if (mapped != null) {
                results.add(mapped);
            }
        }
    }

    private boolean shouldContinuePagination(int currentSize, int maxResults, String nextPage) {
        return currentSize < maxResults && nextPage != null;
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

    // --- DTOs for GET /v1/me/playlists ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistListResponse(List<SpotifySimplifiedPlaylist> items, String next) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifySimplifiedPlaylist(
            String id,
            String name,
            List<SpotifyImage> images,
            SpotifyOwner owner,
            boolean collaborative,
            SpotifyPlaylistItems items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyOwner(String id, @JsonProperty("display_name") String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistItems(int total) {}

    // --- DTO for GET /v1/me ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyCurrentUser(String id) {}
}
