package com.example.spotifysmartifybe.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class BearerTokenResolver {

    private static final int BEARER_PREFIX_LENGTH = "Bearer ".length();

    private BearerTokenResolver() {}

    public static String requireBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }
        return authHeader.substring(BEARER_PREFIX_LENGTH);
    }
}
