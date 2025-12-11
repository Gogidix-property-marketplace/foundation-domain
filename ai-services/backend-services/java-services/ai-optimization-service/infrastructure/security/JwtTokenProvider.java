package com.gogidix.ai.optimization.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT token provider for authentication and authorization.
 * Handles JWT token creation, validation, and parsing.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt.secret:EnterpriseTestService-default-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration:86400}")
    private Long jwtExpiration;

    @Value("${app.security.jwt.refresh-expiration:604800}")
    private Long jwtRefreshExpiration;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates JWT token for authentication.
     *
     * @param authentication the authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateToken(userPrincipal.getUsername());
    }

    /**
     * Generates JWT token for username.
     *
     * @param username the username
     * @return JWT token string
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generates refresh token.
     *
     * @param username the username
     * @return refresh token string
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpiration * 1000);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extracts username from JWT token.
     *
     * @param token the JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Extracts expiration date from JWT token.
     *
     * @param token the JWT token
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    /**
     * Validates JWT token.
     *
     * @param token the JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Checks if token is expired.
     *
     * @param token the JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Refreshes JWT token.
     *
     * @param token the old JWT token
     * @return new JWT token
     */
    public String refreshToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            return generateToken(username);
        } catch (JwtException e) {
            log.error("Failed to refresh token", e);
            return null;
        }
    }

    /**
     * Gets token expiration time in seconds.
     *
     * @return expiration time in seconds
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    /**
     * Gets refresh token expiration time in seconds.
     *
     * @return refresh token expiration time in seconds
     */
    public long getRefreshExpirationTime() {
        return jwtRefreshExpiration;
    }
}