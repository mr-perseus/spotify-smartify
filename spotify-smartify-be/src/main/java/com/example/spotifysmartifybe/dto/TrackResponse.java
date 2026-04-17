package com.example.spotifysmartifybe.dto;

public record TrackResponse(
        String id,
        String name,
        String artists,
        String albumName,
        String albumImageUrl,
        String previewUrl,
        String spotifyUrl
) {}
