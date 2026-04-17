package com.example.spotifysmartifybe.exception;

public class SpotifyApiException extends RuntimeException {

    public SpotifyApiException(String message) {
        super(message);
    }

    public SpotifyApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
