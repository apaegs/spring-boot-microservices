package org.example.bff.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Returns information about the currently authenticated user.
 * Used by the frontend to distinguish the user's own messages from others'.
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, String> me(@AuthenticationPrincipal OAuth2User principal) {
        // getName() returns the 'sub' claim (user-name-attribute=sub in application.properties).
        // Message service also identifies senders by sub, so the isOwn check aligns.
        String username = principal != null ? principal.getName() : "unknown";
        return Map.of("username", username);
    }
}
