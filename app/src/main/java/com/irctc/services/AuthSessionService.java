package com.irctc.services;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    private static final long SESSION_DURATION_SECONDS = 30 * 60;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, SessionData> sessions =
            new ConcurrentHashMap<>();

    /**
     * Creates a secure authentication token for a user.
     *
     * @param userId authenticated user's ID
     * @return generated authentication token
     */
    public String createSession(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID cannot be empty."
            );
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        Instant expiresAt = Instant.now()
                .plusSeconds(SESSION_DURATION_SECONDS);

        sessions.put(
                token,
                new SessionData(userId, expiresAt)
        );

        return token;
    }

    /**
     * Validates a token and returns its associated user ID.
     *
     * @param token authentication token
     * @return user ID if valid; otherwise null
     */
    public String validateSession(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        SessionData session = sessions.get(token);

        if (session == null) {
            return null;
        }

        if (Instant.now().isAfter(session.expiresAt)) {
            sessions.remove(token, session);
            return null;
        }

        return session.userId;
    }

    /**
     * Invalidates a user's session during logout.
     *
     * @param token authentication token
     * @return true if the session existed and was removed
     */
    public boolean invalidateSession(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        return sessions.remove(token) != null;
    }

    /**
     * Stores the data associated with a session.
     */
    private static class SessionData {

        private final String userId;
        private final Instant expiresAt;

        private SessionData(String userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}