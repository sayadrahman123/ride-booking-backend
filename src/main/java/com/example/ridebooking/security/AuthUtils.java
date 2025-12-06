package com.example.ridebooking.security;

import com.example.ridebooking.entity.User;
import com.example.ridebooking.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Small util to fetch the currently authenticated User entity from the SecurityContext.
 * Assumes the username in UserDetails is the email (that's how we built it).
 */
@Component
public class AuthUtils {

    private final UserRepository userRepository;

    public AuthUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns Optional<User> for the currently authenticated principal (by email).
     */
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object principal = auth.getPrincipal();
        String email = null;
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            email = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        }
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    /**
     * Convenience: return user id if present, else throw IllegalStateException.
     */
    public Long requireCurrentUserId() {
        return getCurrentUser().map(User::getId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }
}
