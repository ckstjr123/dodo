package com.dodo.todo.auth.social.client;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;

public interface GoogleAuthClient {

    OAuthUserInfo authenticate(String authorizationCode, String redirectUri);
}
