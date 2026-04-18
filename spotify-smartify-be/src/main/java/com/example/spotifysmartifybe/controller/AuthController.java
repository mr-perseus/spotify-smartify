package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.RefreshRequest;
import com.example.spotifysmartifybe.dto.RefreshResponse;
import com.example.spotifysmartifybe.service.AuthService;
import com.example.spotifysmartifybe.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserService userService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.allowed-spotify-ids:}")
    private String allowedSpotifyIdsRaw;

    private List<String> allowedSpotifyIds;

    @PostConstruct
    void init() {
        allowedSpotifyIds = (allowedSpotifyIdsRaw == null || allowedSpotifyIdsRaw.isBlank())
                ? List.of()
                : Arrays.stream(allowedSpotifyIdsRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        return ResponseEntity.ok(Map.of("authorizationUrl", authService.getAuthorizationUri().toString()));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        if (error != null || code == null) {
            return redirectToFrontend(UriComponentsBuilder.fromPath("/")
                    .queryParam("error", error != null ? error : "missing_code")
                    .toUriString());
        }

        try {
            AuthorizationCodeCredentials credentials = authService.exchangeCode(code);

            if (!isUserAllowed(credentials.getAccessToken())) {
                    return redirectToFrontend(UriComponentsBuilder.fromPath("/")
                            .queryParam("error", "access_denied")
                            .toUriString());
            }

            return redirectToFrontend(UriComponentsBuilder.fromPath("/callback")
                    .queryParam("accessToken", credentials.getAccessToken())
                    .queryParam("refreshToken", credentials.getRefreshToken())
                    .queryParam("expiresIn", credentials.getExpiresIn())
                    .toUriString());

        } catch (Exception e) {
            log.error("Error during OAuth callback: {}", e.getMessage(), e);
            return redirectToFrontend(UriComponentsBuilder.fromPath("/")
                    .queryParam("error", "exchange_failed")
                    .toUriString());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request)
            throws TooManyRequestsException {
        String refreshToken = request.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthorizationCodeCredentials credentials = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(new RefreshResponse(
                credentials.getAccessToken(),
                credentials.getExpiresIn()
        ));
    }

    private ResponseEntity<Void> redirectToFrontend(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + path));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    private boolean isUserAllowed(String accessToken) throws UnauthorizedException, TooManyRequestsException {
        if (allowedSpotifyIds.isEmpty()) {
            return true;
        }
        String spotifyId = userService.getCurrentUserProfile(accessToken).getId();
        return allowedSpotifyIds.contains(spotifyId);
    }
}
