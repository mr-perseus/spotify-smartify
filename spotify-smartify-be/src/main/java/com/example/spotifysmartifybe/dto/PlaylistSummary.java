package com.example.spotifysmartifybe.dto;

public record PlaylistSummary(
        String id,
        String name,
        String imageUrl,
        int trackCount,
        String ownerName,
        boolean collaborative
) {}
