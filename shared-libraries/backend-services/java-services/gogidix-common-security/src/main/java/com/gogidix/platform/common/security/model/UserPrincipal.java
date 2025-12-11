package com.gogidix.platform.common.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * User principal for authentication and authorization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String tenantId;
    private Collection<GrantedAuthority> authorities;
    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private boolean accountNonExpired = true;
    @Builder.Default
    private boolean accountNonLocked = true;
    @Builder.Default
    private boolean credentialsNonExpired = true;
    private Map<String, Object> attributes;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        // Password is not stored in the principal
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return String.format("%s %s", firstName, lastName).trim();
        }
        return username;
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        if (role.startsWith("ROLE_")) {
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals(role));
        } else {
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
        }
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... roles) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> {
                    for (String role : roles) {
                        if (role.startsWith("ROLE_")) {
                            if (authority.equals(role)) return true;
                        } else {
                            if (authority.equals("ROLE_" + role)) return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Check if user has specific authority
     */
    public boolean hasAuthority(String authority) {
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals(authority));
    }

    /**
     * Generate random user ID if not set
     */
    public String getUserId() {
        if (userId == null) {
            userId = UUID.randomUUID().toString();
        }
        return userId;
    }
}