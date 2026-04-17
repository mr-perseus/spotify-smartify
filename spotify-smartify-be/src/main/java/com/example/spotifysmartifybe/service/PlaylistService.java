package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.dto.PlaylistResponse;
import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final int PAGE_LIMIT = 100;
    private static final int MAX_TRACKS = 500;

    private final SpotifyApiFactory spotifyApiFactory;

    public PlaylistResponse getPlaylistWithTracks(String accessToken, String playlistId)
            throws UnauthorizedException, NotFoundException {

        SpotifyApi api = spotifyApiFactory.createWithAccessToken(accessToken);

        String playlistName;
        try {
            playlistName = api.getPlaylist(playlistId).build().execute().getName();
        } catch (UnauthorizedException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to fetch playlist info", e);
        }

        List<Track> allTracks = new ArrayList<>();
        int offset = 0;
        try {
            while (allTracks.size() < MAX_TRACKS) {
                Paging<PlaylistTrack> page = api.getPlaylistsItems(playlistId)
                        .limit(PAGE_LIMIT)
                        .offset(offset)
                        .build()
                        .execute();

                for (PlaylistTrack pt : page.getItems()) {
                    if (pt != null && !Boolean.TRUE.equals(pt.getIsLocal())
                            && pt.getTrack() instanceof Track track) {
                        allTracks.add(track);
                    }
                }

                if (page.getNext() == null) break;
                offset += PAGE_LIMIT;
            }
        } catch (UnauthorizedException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to fetch playlist tracks", e);
        }

        List<TrackResponse> trackResponses = allTracks.stream()
                .map(track -> new TrackResponse(
                        track.getId(),
                        track.getName(),
                        Arrays.stream(track.getArtists())
                                .map(ArtistSimplified::getName)
                                .collect(java.util.stream.Collectors.joining(", ")),
                        track.getAlbum().getName(),
                        track.getAlbum().getImages().length > 0
                                ? track.getAlbum().getImages()[0].getUrl()
                                : "",
                        track.getPreviewUrl() != null ? track.getPreviewUrl() : "",
                        track.getExternalUrls().get("spotify")
                ))
                .toList();

        return new PlaylistResponse(playlistName, trackResponses);
    }
}
