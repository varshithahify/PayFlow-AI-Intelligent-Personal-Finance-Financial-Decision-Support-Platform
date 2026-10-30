package com.payflow.payflow_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Test-only secret. Never use this secret in production.
        String testSecret =
                "PayFlowAITestSecretKeyMustBeAtLeast32BytesLong!";

        jwtService = new JwtService(
                testSecret,
                3_600_000L
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
                3_600_000L
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
}

