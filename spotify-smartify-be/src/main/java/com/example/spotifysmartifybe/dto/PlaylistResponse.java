package com.example.spotifysmartifybe.dto;

import java.util.List;

public record PlaylistResponse(String playlistName, List<TrackResponse> tracks) {}
