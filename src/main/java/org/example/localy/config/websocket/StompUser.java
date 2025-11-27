package org.example.localy.config.websocket;

import java.security.Principal;

public record StompUser(Long userId, String email) implements Principal {
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}

