package com.kumple.dto;

public record AuthUserResponse(
        boolean authenticated,
        String subject,
        String displayName,
        String email
) {
    public static AuthUserResponse anonymous() {
        return new AuthUserResponse(false, null, null, null);
    }
}
