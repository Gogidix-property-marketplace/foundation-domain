package com.gogidix.platform.common.security.jwt;

import com.gogidix.platform.common.security.config.SecurityProperties;
import com.gogidix.platform.common.security.model.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT token provider for authentication and authorization
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final SecurityProperties securityProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT token for authentication
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant validity = now.plus(securityProperties.getJwt().getExpiration());

        return Jwts.builder()
                .setSubject(userPrincipal.getUserId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .setIssuer(securityProperties.getJwt().getIssuer())
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", userPrincipal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("tenantId", userPrincipal.getTenantId())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant validity = now.plus(securityProperties.getJwt().getRefreshExpiration());

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .setIssuer(securityProperties.getJwt().getIssuer())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse and validate JWT token
     */
    public Jws<Claims> parseToken(String token) {
        try {
            // Use JJWT 0.12.x API
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature", ex);
            throw new JwtException("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token", ex);
            throw new JwtException("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token", ex);
            throw new JwtException("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token", ex);
            throw new JwtException("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty", ex);
            throw new JwtException("JWT claims string is empty");
        }
    }

    /**
     * Get authentication from token
     */
    public Authentication getAuthentication(String token) {
        Jws<Claims> claims = parseToken(token);

        String userId = claims.getBody().getSubject();
        String username = claims.getBody().get("username", String.class);
        String email = claims.getBody().get("email", String.class);
        String tenantId = claims.getBody().get("tenantId", String.class);

        @SuppressWarnings("unchecked")
        Collection<String> roles = claims.getBody().get("roles", Collection.class);

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserPrincipal userPrincipal = UserPrincipal.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .tenantId(tenantId)
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(userPrincipal, token, authorities);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException ex) {
            log.debug("JWT token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Get token expiration date
     */
    public Date getTokenExpiration(String token) {
        Jws<Claims> claims = parseToken(token);
        return claims.getBody().getExpiration();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getTokenExpiration(token);
            return expiration.before(new Date());
        } catch (JwtException ex) {
            return true;
        }
    }

    /**
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        Jws<Claims> claims = parseToken(token);
        return claims.getBody().get("username", String.class);
    }

    /**
     * Get user ID from token
     */
    public String getUserIdFromToken(String token) {
        Jws<Claims> claims = parseToken(token);
        return claims.getBody().getSubject();
    }

    /**
     * Spring Security JWT authentication converter
     */
    public org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String userId = jwt.getSubject();
            String username = jwt.getClaimAsString("username");
            String email = jwt.getClaimAsString("email");
            String tenantId = jwt.getClaimAsString("tenantId");

            Collection<String> roles = jwt.getClaim("roles");
            if (roles == null) {
                roles = Arrays.asList("ROLE_USER");
            }

            Collection<GrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .tenantId(tenantId)
                    .authorities(authorities)
                    .build();

            return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                    jwt, authorities, username);
        };
    }
}