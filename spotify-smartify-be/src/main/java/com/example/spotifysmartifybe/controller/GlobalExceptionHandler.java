package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.exception.SpotifyApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("forbidden", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("not_found", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(SpotifyApiException.class)
    public ResponseEntity<Map<String, String>> handleSpotifyApiException(SpotifyApiException ex) {
        log.error("Spotify API exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("spotify_api_error", ex.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyRequests(TooManyRequestsException ex) {
        return rateLimitResponse(ex.getRetryAfter(), ex);
    }

    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<Map<String, String>> handleHttpTooManyRequests(HttpClientErrorException.TooManyRequests ex) {
        String retryAfter = ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().getFirst("Retry-After") : null;
        return rateLimitResponse(retryAfter != null ? Integer.parseInt(retryAfter) : 30, ex);
    }

    private Map<String, String> errorBody(String error, String message) {
        return Map.of("error", error, "message", message);
    }

    private ResponseEntity<Map<String, String>> rateLimitResponse(int retrySeconds, Exception ex) {
        log.warn("Spotify rate limit hit, retry after {} seconds", retrySeconds, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(retrySeconds));
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of("error", "rate_limited",
                        "message", "Spotify rate limit reached. Try again in " + retrySeconds + " seconds.",
                        "retryAfter", String.valueOf(retrySeconds)));
    }
}
