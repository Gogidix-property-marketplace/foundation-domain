package com.gogidix.foundation.dynamic.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // In a real implementation, you would load the user from a database
        // For now, we'll create a simple user with roles based on username
        List<SimpleGrantedAuthority> authorities;

        if (username.equals("admin")) {
            authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_DYNAMIC_CONFIG_ADMIN"),
                new SimpleGrantedAuthority("ROLE_DYNAMIC_CONFIG_READER")
            );
        } else if (username.startsWith("config-")) {
            authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_DYNAMIC_CONFIG_ADMIN"),
                new SimpleGrantedAuthority("ROLE_DYNAMIC_CONFIG_READER")
            );
        } else {
            authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_DYNAMIC_CONFIG_READER")
            );
        }

        return new User(username, "password", authorities);
    }
}