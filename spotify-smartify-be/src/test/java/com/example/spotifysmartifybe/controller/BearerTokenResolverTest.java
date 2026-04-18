package com.example.spotifysmartifybe.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerTokenResolverTest {

    @Test
    void validBearerToken_returnsToken() {
        String token = BearerTokenResolver.requireBearerToken("Bearer abc123");
        assertThat(token).isEqualTo("abc123");
    }

    @Test
    void nullHeader_throwsUnauthorized() {
      //noinspection DataFlowIssue
      assertThatThrownBy(() -> BearerTokenResolver.requireBearerToken(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    void missingPrefix_throwsUnauthorized() {
        assertThatThrownBy(() -> BearerTokenResolver.requireBearerToken("Basic abc123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    void emptyHeader_throwsUnauthorized() {
        assertThatThrownBy(() -> BearerTokenResolver.requireBearerToken(""))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }
}
