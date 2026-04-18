package com.example.spotifysmartifybe.service;

import com.example.spotifysmartifybe.config.SpotifyApiFactory;
import com.example.spotifysmartifybe.dto.TrackResponse;
import com.example.spotifysmartifybe.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final SpotifyApiFactory spotifyApiFactory;

    public User getCurrentUserProfile(String accessToken) throws UnauthorizedException, TooManyRequestsException {
        try {
            return spotifyApiFactory.createWithAccessToken(accessToken)
                    .getCurrentUsersProfile()
                    .build()
                    .execute();
        } catch (UnauthorizedException | TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch user profile", e);
            throw new SpotifyApiException("Failed to fetch user profile", e);
        }
    }

    public List<TrackResponse> getTopTracks(String accessToken, String timeRange) throws UnauthorizedException, TooManyRequestsException {
        try {
            Track[] items = spotifyApiFactory.createWithAccessToken(accessToken)
                    .getUsersTopTracks()
                    .limit(50)
                    .time_range(timeRange)
                    .build()
                    .execute()
                    .getItems();

            if (items == null) {
                return List.of();
            }

            return Arrays.stream(items)
                    .map(UserService::toTrackResponse)
                    .toList();
        } catch (UnauthorizedException | TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch top tracks", e);
            throw new SpotifyApiException("Failed to fetch top tracks", e);
        }
    }

    private static TrackResponse toTrackResponse(Track track) {
        String artistNames = (track.getArtists() != null)
                ? Arrays.stream(track.getArtists())
                        .map(ArtistSimplified::getName)
                        .collect(Collectors.joining(", "))
                : "";

        String albumName = (track.getAlbum() != null) ? track.getAlbum().getName() : "";

        String albumImageUrl = "";
        if (track.getAlbum() != null && track.getAlbum().getImages() != null
                && track.getAlbum().getImages().length > 0) {
            albumImageUrl = track.getAlbum().getImages()[0].getUrl();
        }

        String previewUrl = (track.getPreviewUrl() != null) ? track.getPreviewUrl() : "";

        String spotifyUrl = "";
        if (track.getExternalUrls() != null && track.getExternalUrls().get("spotify") != null) {
            spotifyUrl = track.getExternalUrls().get("spotify");
        }

        return new TrackResponse(
                track.getId(), track.getName(), artistNames, albumName,
                albumImageUrl, previewUrl, spotifyUrl);
    }
}
