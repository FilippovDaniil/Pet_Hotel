package com.pethotel.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-min-32-chars-here-ok";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        assertThat(jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER")).isNotBlank();
    }

    @Test
    void getUserId_roundTrip() {
        String token = jwtUtil.generateToken(42L, "user@example.com", "CUSTOMER");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void getEmail_roundTrip() {
        String token = jwtUtil.generateToken(1L, "test@hotel.com", "ADMIN");
        assertThat(jwtUtil.getEmail(token)).isEqualTo("test@hotel.com");
    }

    @Test
    void getRole_roundTrip() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "RECEPTION");
        assertThat(jwtUtil.getRole(token)).isEqualTo("RECEPTION");
    }

    @Test
    void isValid_trueForValidToken() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_falseForExpiredToken() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L);
        String token = shortLived.generateToken(1L, "user@example.com", "CUSTOMER");
        Thread.sleep(20);
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void isValid_falseForTamperedSignature() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "CUSTOMER");
        String tampered = token.substring(0, token.length() - 6) + "XXXXXX";
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_falseForDifferentSecret() {
        JwtUtil other = new JwtUtil("other-secret-key-min-32-chars-here-ok", EXPIRATION_MS);
        String token = other.generateToken(1L, "user@example.com", "CUSTOMER");
        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void isValid_falseForGarbage() {
        assertThat(jwtUtil.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void parseToken_containsAllClaims() {
        String token = jwtUtil.generateToken(7L, "admin@hotel.com", "ADMIN");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("admin@hotel.com");
        assertThat(claims.get("userId", Long.class)).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }
}
