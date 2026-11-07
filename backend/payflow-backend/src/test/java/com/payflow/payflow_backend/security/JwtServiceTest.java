package com.payflow.payflow_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "PayFlowAITestSecretKeyMustBeAtLeast32BytesLong!";

    private static final long EXPIRATION_TIME = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION_TIME
        );
    }

    @Test
    void shouldGenerateAndValidateToken() {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT"
        );

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void shouldExtractEmailFromToken() {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT"
        );

        assertEquals(
                "merchant@example.com",
                jwtService.extractEmail(token)
        );
    }

    @Test
    void shouldExtractOrganizationIdFromToken() {

        String token = jwtService.generateToken(
                "merchant@example.com",
                2L,
                "MERCHANT"
        );

        assertEquals(
                2L,
                jwtService.extractOrgId(token)
        );
    }

    @Test
    void shouldExtractRoleFromToken() {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT"
        );

        assertEquals(
                "MERCHANT",
                jwtService.extractRole(token)
        );
    }

    @Test
    void shouldRejectMalformedToken() {

        assertFalse(
                jwtService.isTokenValid("invalid.jwt.token")
        );
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {

        JwtService anotherJwtService = new JwtService(
                "AnotherTestSecretKeyMustAlsoBeAtLeast32Bytes!",
                EXPIRATION_TIME
        );

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT"
        );

        assertFalse(
                anotherJwtService.isTokenValid(token)
        );
    }

    @Test
    void shouldRejectExpiredToken() {

        String expiredToken = Jwts.builder()
                .subject("merchant@example.com")
                .claim("orgId", 1L)
                .claim("role", "MERCHANT")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(getSecretKey())
                .compact();

        assertFalse(
                jwtService.isTokenValid(expiredToken)
        );
    }

    @Test
    void shouldRejectEmptyToken() {

        assertFalse(
                jwtService.isTokenValid("")
        );
    }

    @Test
    void shouldRejectTamperedToken() {

        String token = jwtService.generateToken(
                "merchant@example.com",
                1L,
                "MERCHANT"
        );

        String tamperedToken = token.substring(0, token.length() - 2)
                + "ab";

        assertFalse(
                jwtService.isTokenValid(tamperedToken)
        );
    }

    @Test
    void shouldRejectTokenWithMissingOrganizationClaim() {

        String token = Jwts.builder()
                .subject("merchant@example.com")
                .claim("role", "MERCHANT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSecretKey())
                .compact();

        assertFalse(
                jwtService.isTokenValid(token)
        );
    }

    @Test
    void shouldRejectTokenWithMissingRoleClaim() {

        String token = Jwts.builder()
                .subject("merchant@example.com")
                .claim("orgId", 1L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSecretKey())
                .compact();

        assertFalse(
                jwtService.isTokenValid(token)
        );
    }

    @Test
    void shouldRejectTokenWithMissingEmail() {

        String token = Jwts.builder()
                .claim("orgId", 1L)
                .claim("role", "MERCHANT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSecretKey())
                .compact();

        assertFalse(
                jwtService.isTokenValid(token)
        );
    }

    private SecretKey getSecretKey() {

        return Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }
}
