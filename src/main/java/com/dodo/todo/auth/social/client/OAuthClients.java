package com.dodo.todo.auth.social.client;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthClients {

    private final List<OAuthClient> clients;

    /**
     * 소셜 제공자를 지원하는 OAuth 클라이언트로 인증을 위임한다.
     */
    public OAuthUserInfo authenticate(SocialProvider provider, String authorizationCode, String redirectUri) {
        OAuthClient client = clients.stream()
                .filter(oAuthClient -> oAuthClient.supports(provider))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "UNSUPPORTED_SOCIAL_PROVIDER",
                        HttpStatus.BAD_REQUEST,
                        "Unsupported social provider"
                ));

        return client.authenticate(authorizationCode, redirectUri);
    }
}
