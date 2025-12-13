package com.example.ridebooking.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {

    /**
     * Attempts to return the current authenticated user's id as Long.
     * Expects the Authentication.getName() to be numeric or the principal to expose getId().
     * Adjust this method to match your security principal (JWT claim names, UserDetails, etc.)
     */
    public static Long requireCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }

        // Try a few strategies:
        Object principal = auth.getPrincipal();
        // 1) principal is a String name (maybe userId)
        if (principal instanceof String) {
            String name = (String) principal;
            try {
                return Long.valueOf(name);
            } catch (NumberFormatException ignored) {}
        }

        // 2) principal may be a custom UserDetails exposing getId(); attempt reflectively
        try {
            java.lang.reflect.Method m = principal.getClass().getMethod("getId");
            Object id = m.invoke(principal);
            if (id instanceof Number) return ((Number) id).longValue();
            if (id instanceof String) return Long.valueOf((String) id);
        } catch (Exception ignored) {}

        // 3) fallback to authentication name
        String name = auth.getName();
        try {
            return Long.valueOf(name);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unable to resolve current user id from authentication principal");
        }
    }
}
