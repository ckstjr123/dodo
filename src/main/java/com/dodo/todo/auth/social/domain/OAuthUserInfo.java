package com.dodo.todo.auth.social.domain;

public record OAuthUserInfo(
        SocialProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified
) {
}
