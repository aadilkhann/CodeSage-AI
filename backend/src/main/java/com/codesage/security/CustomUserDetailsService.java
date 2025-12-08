package com.codesage.security;

import com.codesage.model.User;
import com.codesage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

/**
 * CustomUserDetailsService - Load user details for Spring Security
 * 
 * THOUGHT PROCESS:
 * 
 * 1. Why UserDetailsService?
 * - Spring Security interface for loading user data
 * - Called by authentication filter
 * - Bridges our User entity with Spring Security
 * 
 * 2. What is UserDetails?
 * - Spring Security's representation of a user
 * - Contains username, password, authorities (roles)
 * - We adapt our User entity to this interface
 * 
 * 3. Why load by UUID string?
 * - JWT contains user ID (UUID)
 * - We pass UUID.toString() to this method
 * - Parse back to UUID to query database
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username (actually user ID in our case)
     * 
     * THOUGHT PROCESS:
     * - "username" parameter is actually user ID (UUID as string)
     * - Why? JWT filter passes userId.toString()
     * - Load user from database
     * - Convert to Spring Security UserDetails
     * 
     * @param username User ID as string
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Parse username as UUID
        UUID userId;
        try {
            userId = UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid user ID format: " + username);
        }

        // Load user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        // Convert to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password("") // No password needed (OAuth authentication)
                .authorities(new ArrayList<>()) // No roles yet (can add later)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
