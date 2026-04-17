package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * GET /auth/login
     * Returns the Spotify authorization URL the client should redirect the user to.
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        URI authUri = authService.getAuthorizationUri();
        return ResponseEntity.ok(Map.of("authorizationUrl", authUri.toString()));
    }

    /**
     * GET /auth/callback?code=...&state=...
     * Spotify redirects here after the user grants (or denies) access.
     * Exchanges the authorization code for access + refresh tokens and returns them.
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        if (error != null || code == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", error != null ? error : "Missing authorization code"));
        }

        AuthorizationCodeCredentials credentials = authService.exchangeCode(code);

        return ResponseEntity.ok(Map.of(
                "accessToken", credentials.getAccessToken(),
                "refreshToken", credentials.getRefreshToken(),
                "expiresIn", credentials.getExpiresIn()
        ));
    }
}
