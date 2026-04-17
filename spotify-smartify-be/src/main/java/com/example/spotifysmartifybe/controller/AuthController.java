package com.example.spotifysmartifybe.controller;

import com.example.spotifysmartifybe.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
     * GET /auth/callback?code=...
     * Spotify redirects the browser here. Exchanges the code for tokens, then
     * performs a browser redirect to the React frontend /callback route.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        HttpHeaders headers = new HttpHeaders();

        if (error != null || code == null) {
            String reason = error != null ? error : "missing_code";
            headers.setLocation(URI.create(frontendUrl + "/callback?error=" + reason));
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        }

        try {
            AuthorizationCodeCredentials credentials = authService.exchangeCode(code);
            String location = frontendUrl + "/callback"
                    + "?accessToken=" + credentials.getAccessToken()
                    + "&refreshToken=" + credentials.getRefreshToken()
                    + "&expiresIn=" + credentials.getExpiresIn();
            headers.setLocation(URI.create(location));
        } catch (Exception e) {
            headers.setLocation(URI.create(frontendUrl + "/callback?error=exchange_failed"));
        }

        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
