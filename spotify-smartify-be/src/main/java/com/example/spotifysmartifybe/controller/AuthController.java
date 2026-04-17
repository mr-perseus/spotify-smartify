package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.dto.RefreshResponse;
import com.example.spotifysmartifybe.service.AuthService;
import com.example.spotifysmartifybe.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

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
            return redirectToFrontend("/?error=" + (error != null ? error : "missing_code"));
        }

        try {
            AuthorizationCodeCredentials credentials = authService.exchangeCode(code);

            if (!allowedSpotifyIds.isEmpty()) {
                String spotifyId = userService.getCurrentUserProfile(credentials.getAccessToken()).getId();
                if (!allowedSpotifyIds.contains(spotifyId)) {
                    return redirectToFrontend("/?error=access_denied");
                }
            }

            return redirectToFrontend("/callback"
                    + "?accessToken=" + credentials.getAccessToken()
                    + "&refreshToken=" + credentials.getRefreshToken()
                    + "&expiresIn=" + credentials.getExpiresIn());

        } catch (Exception e) {
            return redirectToFrontend("/?error=exchange_failed");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
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
}
