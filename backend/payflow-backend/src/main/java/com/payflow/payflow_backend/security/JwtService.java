package com.payflow.payflow_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expirationTime = expirationTime;
    }

    public String generateToken(
            String email,
            Long orgId,
            String role
    ) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + expirationTime
        );

        return Jwts.builder()
                .subject(email)
                .claim("orgId", orgId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Long extractOrgId(String token) {

        Object orgId = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("orgId");

        if (orgId instanceof Number number) {
            return number.longValue();
        }

        if (orgId instanceof String value) {
            return Long.parseLong(value);
        }

        throw new IllegalArgumentException("Invalid orgId claim");
    }

    public String extractRole(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {

        try {
            extractEmail(token);
            extractOrgId(token);
            extractRole(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}