package com.dodo.todo.auth.social.client;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;

public interface OAuthClient {

    boolean supports(SocialProvider provider);

    OAuthUserInfo authenticate(String accessToken);
}
