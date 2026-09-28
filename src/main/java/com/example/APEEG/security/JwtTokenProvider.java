package com.example.APEEG.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utility component responsible for generating, parsing, and validating JWT tokens.
 */
@Component
public class JwtTokenProvider {

    // 256-bit secret key generated for HMAC-SHA signing
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token validity duration in milliseconds (Default: 24 hours)
    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /**
     * Generates a signed JWT token containing the scientist's personId and email.
     *
     * @param personId Unique Mongo ID of the authenticated scientist
     * @param email    Official email of the scientist
     * @return Compacted, signed JWT string
     */
    public String generateToken(String personId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(personId)               // Sets personId as subject claim
                .claim("email", email)             // Custom claim storing scientist email
                .setIssuedAt(now)                   // Issue timestamp
                .setExpiration(expiryDate)         // Expiry timestamp
                .signWith(key)                      // Digital signature
                .compact();
    }

    /**
     * Extracts the Person ID (subject) encoded within a JWT token.
     */
    public String getPersonIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Validates the integrity and expiration status of an incoming JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is expired, malformed, or signature does not match
            return false;
        }
    }
}