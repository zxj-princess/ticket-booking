package com.ticket.config;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/** Single-instance sessions. Restart invalidates tokens; never trust client user IDs. */
@Service
public class SessionService {
    private record Session(Long userId, long expiresAt) {}
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    public String create(Long userId) {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(userId, now + 8 * 60 * 60 * 1000L));
        return token;
    }
    public Long resolve(String token) {
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (session.expiresAt() <= System.currentTimeMillis()) {
            sessions.remove(token);
            return null;
        }
        return session.userId();
    }
    public void revoke(String token) { if (token != null) sessions.remove(token); }
}
