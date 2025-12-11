package com.gogidix.platform.common.security.jwt;

import com.gogidix.platform.common.security.config.SecurityProperties;
import com.gogidix.platform.common.security.model.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.getJwt().setSecret("test-secret-key-for-jwt-token-generation-and-validation-123456");
        securityProperties.getJwt().setExpiration(Duration.ofHours(1));
        securityProperties.getJwt().setRefreshExpiration(Duration.ofDays(7));
        securityProperties.getJwt().setIssuer("gogidix-test");

        tokenProvider = new JwtTokenProvider(securityProperties);
    }

    @Test
    void testGenerateAndValidateToken() {
        // Given
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        UserPrincipal userPrincipal = UserPrincipal.builder()
                .userId("user123")
                .username("testuser")
                .email("test@example.com")
                .tenantId("tenant1")
                .build();

        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userPrincipal, null, Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );

        // When
        String token = tokenProvider.generateToken(authentication);

        // Then
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("testuser", tokenProvider.getUsernameFromToken(token));
        assertEquals("user123", tokenProvider.getUserIdFromToken(token));
    }

    @Test
    void testInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThrows(Exception.class, () -> tokenProvider.parseToken(invalidToken));
        assertFalse(tokenProvider.validateToken(invalidToken));
    }

    @Test
    void testGenerateRefreshToken() {
        // Given
        String userId = "user123";

        // When
        String refreshToken = tokenProvider.generateRefreshToken(userId);

        // Then
        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertEquals(userId, tokenProvider.getUserIdFromToken(refreshToken));
    }
}