package com.kumple.controller;

import com.kumple.dto.AuthUserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return AuthUserResponse.anonymous();
        }

        if (authentication.getPrincipal() instanceof OAuth2User user) {
            return new AuthUserResponse(
                    true,
                    authentication.getName(),
                    firstPresent(user, "name", "login", "preferred_username"),
                    firstPresent(user, "email")
            );
        }

        return new AuthUserResponse(true, authentication.getName(), authentication.getName(), null);
    }

    private String firstPresent(OAuth2User user, String... attributes) {
        for (String attribute : attributes) {
            Object value = user.getAttribute(attribute);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
